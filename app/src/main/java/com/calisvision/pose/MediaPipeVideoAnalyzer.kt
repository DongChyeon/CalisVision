package com.calisvision.pose

import android.content.Context
import android.net.Uri
import com.calisvision.domain.model.AnalysisResult
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPipeVideoAnalyzer(private val context: Context) : VideoAnalyzer {

    override suspend fun analyze(uri: Uri): AnalysisResult = withContext(Dispatchers.Default) {
        createLandmarker().use { _ ->
            // TODO(Phase 2): sample frames via VideoSource, run detectForVideo, apply rules.
            AnalysisResult(videoUri = uri.toString(), sampledFrames = 0, detectedFrames = 0)
        }
    }

    private fun createLandmarker(): PoseLandmarker {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .build()
            )
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    companion object {
        const val MODEL_ASSET = "pose_landmarker_full.task"
    }
}
