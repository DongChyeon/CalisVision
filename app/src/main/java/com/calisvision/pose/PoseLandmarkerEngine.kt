package com.calisvision.pose

import android.graphics.Bitmap
import com.calisvision.domain.model.FramePose
import com.calisvision.video.FrameSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import java.io.File

/** [width] × [height] is the first decoded (downscaled) frame, 0 × 0 when none decoded. */
data class ProcessedFrames(val poses: List<FramePose>, val width: Int, val height: Int)

class PoseLandmarkerEngine(private val detectorFactory: () -> PoseDetector) {

    suspend fun process(
        source: FrameSource,
        rotationDeg: Int,
        frameDir: File?,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): ProcessedFrames {
        val total = source.info.frameCount
        val poses = Array(total) { FramePose(it, it * FrameSource.SAMPLE_INTERVAL_MS, null) }
        var width = 0
        var height = 0
        frameDir?.mkdirs()
        detectorFactory().use { detector ->
            // Decode on IO while inference runs here; a small buffer bounds the downscaled bitmaps held in flight.
            source.frames().buffer(FRAME_BUFFER).flowOn(Dispatchers.IO).collect { frame ->
                currentCoroutineContext().ensureActive()
                if (height == 0) {
                    width = frame.bitmap.width
                    height = frame.bitmap.height
                }
                try {
                    val landmarks = detector.detectVideo(frame.bitmap, frame.sampleIndex * FrameSource.SAMPLE_INTERVAL_MS, rotationDeg)
                    poses[frame.sampleIndex] = FramePose(frame.sampleIndex, frame.displayTimeMs, landmarks)
                    frameDir?.let { writeJpeg(frame.bitmap, File(it, "${frame.sampleIndex}.jpg")) }
                } finally {
                    frame.bitmap.recycle()
                }
                onProgress(frame.sampleIndex + 1, total)
            }
        }
        return ProcessedFrames(poses.toList(), width, height)
    }

    private fun writeJpeg(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    }

    private companion object {
        const val JPEG_QUALITY = 85
        const val FRAME_BUFFER = 2
    }
}
