package com.calisvision.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.max
import kotlin.math.roundToInt

class RetrieverFrameSource private constructor(
    private val retriever: MediaMetadataRetriever,
    override val info: VideoInfo,
) : FrameSource {

    override fun frameAt(sampleIndex: Int): SampledFrame? {
        val timeMs = sampleIndex * FrameSource.SAMPLE_INTERVAL_MS
        val full = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
        return SampledFrame(sampleIndex, timeMs, downscale(full))
    }

    override fun close() = retriever.release()

    private fun downscale(full: Bitmap): Bitmap {
        val longest = max(full.width, full.height)
        if (longest <= FrameSource.MAX_SIDE_PX) return full
        val scale = FrameSource.MAX_SIDE_PX.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(full, (full.width * scale).roundToInt(), (full.height * scale).roundToInt(), true)
        full.recycle()
        return scaled
    }

    companion object {
        fun open(context: Context, uri: Uri): RetrieverFrameSource {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                fun meta(key: Int) = retriever.extractMetadata(key)?.toLongOrNull() ?: 0L
                val info = VideoInfo(
                    durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION),
                    width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt(),
                    height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt(),
                    rotationDegrees = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt(),
                )
                return RetrieverFrameSource(retriever, info)
            } catch (e: Exception) {
                retriever.release()
                throw e
            }
        }
    }
}
