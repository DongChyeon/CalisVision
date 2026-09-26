package com.calisvision.video

import android.net.Uri

data class VideoHandle(
    val uri: Uri,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
)

interface VideoSource {
    suspend fun openVideo(uri: Uri): VideoHandle
}
