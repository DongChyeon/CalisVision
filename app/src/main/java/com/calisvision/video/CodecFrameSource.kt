package com.calisvision.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Sequential decode (MediaExtractor + MediaCodec, YUV ByteBuffer output); only sampled frames are converted to RGB.
 * A pass over the video costs about one hardware decode per video frame instead of one keyframe seek + full RGB
 * conversion per sample (`getFrameAtTime(OPTION_CLOSEST)`, 171–190 ms/sample on the P1.5 device).
 *
 * Conversion reproduces `getFrameAtTime` on the P1.5 device (mean |ΔRGB| 0.03 at full resolution): limited-range BT.709
 * with nearest (replicated) chroma, then rotation by the container metadata (clockwise) and bilinear downscale to
 * [FrameSource.MAX_SIDE_PX]. A GPU (SurfaceTexture) conversion was tried and differed by ~2.5/255 at edges, which dropped
 * mean visibility on the tune video 0.978 → 0.886 (±1 LSB noise on the reference frames changed nothing).
 *
 * Sample k picks the first decoded frame with pts ≥ k · 100 ms − half a frame interval (the closest frame for constant
 * frame rate; VFR can drift); `displayTimeMs` stays the requested k · 100 ms. Moving backwards, or more than
 * [SEEK_AHEAD_US] forward, seeks to the previous sync frame. Calls must not be concurrent but may come from any thread.
 * Assumes 8-bit 4:2:0 output (10-bit/HDR streams are not handled).
 */
class CodecFrameSource private constructor(
    override val info: VideoInfo,
    private val extractor: MediaExtractor,
    private val codec: MediaCodec,
    private val halfFrameUs: Long,
) : FrameSource {

    private val bufferInfo = MediaCodec.BufferInfo()
    private var inputDone = false
    private var outputDone = false
    private var lastPtsUs = Long.MIN_VALUE
    private val lastFrameUs = info.durationMs * 1000L - 2 * halfFrameUs
    private var pixels = IntArray(0)
    private val planeBytes = Array(3) { ByteArray(0) }

    @Synchronized
    override fun frameAt(sampleIndex: Int): SampledFrame? {
        val timeMs = sampleIndex * FrameSource.SAMPLE_INTERVAL_MS
        // Past the last frame, take the last frame (as OPTION_CLOSEST does).
        val minPtsUs = minOf(timeMs * 1000L, lastFrameUs) - halfFrameUs
        val needSeek = if (lastPtsUs == Long.MIN_VALUE) {
            minPtsUs > SEEK_AHEAD_US
        } else {
            minPtsUs <= lastPtsUs || minPtsUs - lastPtsUs > SEEK_AHEAD_US
        }
        if (needSeek) seek(minPtsUs)
        val bitmap = decodeUntil(minPtsUs) ?: return null
        return SampledFrame(sampleIndex, timeMs, bitmap)
    }

    override fun close() {
        runCatching { codec.stop() }
        codec.release()
        extractor.release()
    }

    private fun seek(targetUs: Long) {
        extractor.seekTo(targetUs.coerceAtLeast(0L), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        codec.flush()
        inputDone = false
        outputDone = false
        lastPtsUs = Long.MIN_VALUE
    }

    /** Decodes forward and converts the first frame with pts ≥ [minPtsUs]; null at end of stream. */
    private fun decodeUntil(minPtsUs: Long): Bitmap? {
        while (!outputDone) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val size = extractor.readSampleData(codec.getInputBuffer(inIndex)!!, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            if (outIndex < 0) continue
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            if (bufferInfo.size == 0 || bufferInfo.presentationTimeUs < minPtsUs) {
                if (bufferInfo.size != 0) lastPtsUs = bufferInfo.presentationTimeUs
                codec.releaseOutputBuffer(outIndex, false)
                continue
            }
            lastPtsUs = bufferInfo.presentationTimeUs
            try {
                return toBitmap(checkNotNull(codec.getOutputImage(outIndex)) { "decoder gave no YUV image" })
            } finally {
                codec.releaseOutputBuffer(outIndex, false)
            }
        }
        return null
    }

    private fun toBitmap(image: Image): Bitmap {
        val crop = image.cropRect
        val w = crop.width()
        val h = crop.height()
        if (pixels.size != w * h) pixels = IntArray(w * h)
        val planes = image.planes
        for (i in 0..2) {
            val buffer = planes[i].buffer
            if (planeBytes[i].size < buffer.remaining()) planeBytes[i] = ByteArray(buffer.remaining())
            buffer.get(planeBytes[i], 0, buffer.remaining())
        }
        val yBytes = planeBytes[0]
        val uBytes = planeBytes[1]
        val vBytes = planeBytes[2]
        val yRow = planes[0].rowStride
        val yPix = planes[0].pixelStride
        val uRow = planes[1].rowStride
        val uPix = planes[1].pixelStride
        val vRow = planes[2].rowStride
        val vPix = planes[2].pixelStride
        for (y in 0 until h) {
            val sy = crop.top + y
            val yBase = sy * yRow
            val uBase = (sy / 2) * uRow
            val vBase = (sy / 2) * vRow
            val out = y * w
            for (x in 0 until w) {
                val sx = crop.left + x
                val luma = Y_LUT[yBytes[yBase + sx * yPix].toInt() and 0xFF]
                val u = uBytes[uBase + (sx / 2) * uPix].toInt() and 0xFF
                val v = vBytes[vBase + (sx / 2) * vPix].toInt() and 0xFF
                val r = clamp(luma + RV_LUT[v])
                val g = clamp(luma - GU_LUT[u] - GV_LUT[v])
                val b = clamp(luma + BU_LUT[u])
                pixels[out + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        var full = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
        if (info.rotationDegrees % 360 != 0) {
            val rotated = Bitmap.createBitmap(full, 0, 0, w, h, Matrix().apply { postRotate(info.rotationDegrees.toFloat()) }, false)
            full.recycle()
            full = rotated
        }
        return downscale(full)
    }

    private fun downscale(full: Bitmap): Bitmap {
        val longest = max(full.width, full.height)
        if (longest <= FrameSource.MAX_SIDE_PX) return full
        val scale = FrameSource.MAX_SIDE_PX.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(full, (full.width * scale).roundToInt(), (full.height * scale).roundToInt(), true)
        full.recycle()
        return scaled
    }

    companion object {
        private const val TIMEOUT_US = 10_000L
        private const val SEEK_AHEAD_US = 1_500_000L
        private const val DEFAULT_FRAME_RATE = 30

        // Limited-range BT.709 (Kr 0.2126, Kb 0.0722) in fixed point; Y_LUT carries the +0.5 rounding term.
        private const val SHIFT = 16
        private const val KR = 0.2126
        private const val KB = 0.0722
        private const val KG = 1 - KR - KB
        private fun fixed(v: Double) = (v * (1 shl SHIFT)).roundToInt()
        private fun chroma(c: Int) = (c - 128) * 255.0 / 224.0
        private val Y_LUT = IntArray(256) { fixed((it - 16) * 255.0 / 219.0) + (1 shl (SHIFT - 1)) }
        private val RV_LUT = IntArray(256) { fixed(chroma(it) * 2 * (1 - KR)) }
        private val GU_LUT = IntArray(256) { fixed(chroma(it) * 2 * KB * (1 - KB) / KG) }
        private val GV_LUT = IntArray(256) { fixed(chroma(it) * 2 * KR * (1 - KR) / KG) }
        private val BU_LUT = IntArray(256) { fixed(chroma(it) * 2 * (1 - KB)) }

        private fun clamp(v: Int) = (v shr SHIFT).coerceIn(0, 255)

        fun open(context: Context, uri: Uri): CodecFrameSource {
            val retriever = MediaMetadataRetriever()
            val info = try {
                retriever.setDataSource(context, uri)
                fun meta(key: Int) = retriever.extractMetadata(key)?.toLongOrNull() ?: 0L
                VideoInfo(
                    durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION),
                    width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt(),
                    height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt(),
                    rotationDegrees = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt(),
                )
            } finally {
                retriever.release()
            }
            val extractor = MediaExtractor()
            var codec: MediaCodec? = null
            try {
                extractor.setDataSource(context, uri, null)
                val track = (0 until extractor.trackCount).first {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                }
                extractor.selectTrack(track)
                val format = extractor.getTrackFormat(track)
                val fps = if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) format.getInteger(MediaFormat.KEY_FRAME_RATE) else DEFAULT_FRAME_RATE
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                codec.configure(format, null, null, 0)
                codec.start()
                return CodecFrameSource(info, extractor, codec, 500_000L / fps.coerceAtLeast(1))
            } catch (e: Exception) {
                codec?.release()
                extractor.release()
                throw e
            }
        }
    }
}
