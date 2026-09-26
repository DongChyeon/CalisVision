package com.calisvision.pose

import android.graphics.Bitmap
import com.calisvision.domain.model.FramePose
import com.calisvision.video.FrameSource
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

class PoseLandmarkerEngine(private val detectorFactory: () -> PoseDetector) {

    suspend fun process(
        source: FrameSource,
        rotationDeg: Int,
        frameDir: File?,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): List<FramePose> {
        val total = source.info.frameCount
        val poses = Array(total) { FramePose(it, it * FrameSource.SAMPLE_INTERVAL_MS, null) }
        frameDir?.mkdirs()
        detectorFactory().use { detector ->
            source.frames().collect { frame ->
                currentCoroutineContext().ensureActive()
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
        return poses.toList()
    }

    private fun writeJpeg(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    }

    private companion object {
        const val JPEG_QUALITY = 85
    }
}
