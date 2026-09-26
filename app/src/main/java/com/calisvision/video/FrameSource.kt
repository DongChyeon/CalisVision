package com.calisvision.video

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.Closeable

data class SampledFrame(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val bitmap: Bitmap,
)

data class VideoInfo(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
) {
    val frameCount: Int get() = ((durationMs + FrameSource.SAMPLE_INTERVAL_MS - 1) / FrameSource.SAMPLE_INTERVAL_MS).toInt()
}

interface FrameSource : Closeable {
    val info: VideoInfo

    fun frameAt(sampleIndex: Int): SampledFrame?

    fun frames(): Flow<SampledFrame> = flow {
        for (i in 0 until info.frameCount) frameAt(i)?.let { emit(it) }
    }

    companion object {
        const val SAMPLE_INTERVAL_MS = 100L
        const val MAX_SIDE_PX = 640
    }
}
