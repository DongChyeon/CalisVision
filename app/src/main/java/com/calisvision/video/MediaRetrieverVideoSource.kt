package com.calisvision.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRetrieverVideoSource(private val context: Context) : VideoSource {

    // TODO(Phase 2): frame sampling (getFrameAtTime / getFramesAtIndex fallback).
    override suspend fun openVideo(uri: Uri): VideoHandle = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            fun meta(key: Int) = retriever.extractMetadata(key)?.toLongOrNull() ?: 0L
            VideoHandle(
                uri = uri,
                durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION),
                width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt(),
                height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt(),
                rotationDegrees = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt(),
            )
        } finally {
            // AutoCloseable/use() is API 29+; minSdk is 28.
            retriever.release()
        }
    }
}
