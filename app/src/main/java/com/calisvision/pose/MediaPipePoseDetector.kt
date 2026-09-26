package com.calisvision.pose

import android.content.Context
import android.graphics.Bitmap
import com.calisvision.domain.model.Landmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class MediaPipePoseDetector(private val context: Context) : PoseDetector {

    private val imageLandmarker = lazy { create(RunningMode.IMAGE) }
    private val videoLandmarker = lazy { create(RunningMode.VIDEO) }

    override fun detectImage(bitmap: Bitmap, rotationDeg: Int): List<Landmark>? =
        imageLandmarker.value.detect(BitmapImageBuilder(bitmap).build(), options(rotationDeg)).toLandmarks(rotationDeg)

    override fun detectVideo(bitmap: Bitmap, timestampMs: Long, rotationDeg: Int): List<Landmark>? =
        videoLandmarker.value.detectForVideo(BitmapImageBuilder(bitmap).build(), options(rotationDeg), timestampMs).toLandmarks(rotationDeg)

    override fun close() {
        if (imageLandmarker.isInitialized()) imageLandmarker.value.close()
        if (videoLandmarker.isInitialized()) videoLandmarker.value.close()
    }

    private fun options(rotationDeg: Int) = ImageProcessingOptions.builder().setRotationDegrees(rotationDeg).build()

    private fun PoseLandmarkerResult.toLandmarks(rotationDeg: Int): List<Landmark>? {
        val pose = landmarks().firstOrNull() ?: return null
        val flip = rotationDeg == 180 && ROTATED_COORDS_ARE_IN_ROTATED_FRAME
        return pose.map { l ->
            Landmark(
                x = if (flip) 1f - l.x() else l.x(),
                y = if (flip) 1f - l.y() else l.y(),
                z = l.z(),
                visibility = l.visibility().orElse(0f),
            )
        }
    }

    private fun create(mode: RunningMode): PoseLandmarker {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL_ASSET).build())
            .setRunningMode(mode)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .build()
        return PoseLandmarker.createFromOptions(context, options)
    }

    companion object {
        const val MODEL_ASSET = "pose_landmarker_full.task"
        private const val MIN_CONFIDENCE = 0.5f

        // TODO(P1.5): confirm landmarks are reported in the rotated frame; if already in the input frame, set false.
        const val ROTATED_COORDS_ARE_IN_ROTATED_FRAME = true
    }
}
