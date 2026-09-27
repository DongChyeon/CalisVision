package com.calisvision.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.calisvision.domain.model.Landmark
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * [modelAssetPath] is an APK asset name or an absolute file path. [delegate] falls back to CPU when the landmarker
 * cannot be created with it; [activeDelegate] reports what the last created landmarker uses.
 */
class MediaPipePoseDetector(
    private val context: Context,
    private val modelAssetPath: String = MODEL_ASSET,
    private val delegate: Delegate = Delegate.CPU,
) : PoseDetector {

    var activeDelegate: Delegate? = null
        private set

    private val imageLandmarker = lazy { create(RunningMode.IMAGE) }
    private val videoLandmarker = lazy { create(RunningMode.VIDEO) }

    override fun detectImage(bitmap: Bitmap, rotationDeg: Int): List<Landmark>? =
        withRotation(bitmap, rotationDeg) { imageLandmarker.value.detect(BitmapImageBuilder(it).build()) }

    override fun detectVideo(bitmap: Bitmap, timestampMs: Long, rotationDeg: Int): List<Landmark>? =
        withRotation(bitmap, rotationDeg) { videoLandmarker.value.detectForVideo(BitmapImageBuilder(it).build(), timestampMs) }

    override fun close() {
        if (imageLandmarker.isInitialized()) imageLandmarker.value.close()
        if (videoLandmarker.isInitialized()) videoLandmarker.value.close()
    }

    /**
     * 180° rotates the bitmap in memory and detects at 0°, then maps back to the input frame with (1 − x, 1 − y).
     * ImageProcessingOptions.setRotationDegrees(180) is not used: it nearly disables detection on device (ADR-0006).
     */
    private inline fun withRotation(bitmap: Bitmap, rotationDeg: Int, detect: (Bitmap) -> PoseLandmarkerResult): List<Landmark>? {
        require(rotationDeg == 0 || rotationDeg == 180) { "rotationDeg must be 0 or 180, was $rotationDeg" }
        if (rotationDeg == 0) return detect(bitmap).toLandmarks(flip = false)
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(180f) }, true)
        try {
            return detect(rotated).toLandmarks(flip = true)
        } finally {
            rotated.recycle()
        }
    }

    private fun PoseLandmarkerResult.toLandmarks(flip: Boolean): List<Landmark>? {
        val pose = landmarks().firstOrNull() ?: return null
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
        if (delegate != Delegate.CPU) {
            try {
                return create(mode, delegate)
            } catch (e: Exception) {
                Log.w(TAG, "$delegate delegate unavailable for $mode, falling back to CPU", e)
            }
        }
        return create(mode, Delegate.CPU)
    }

    private fun create(mode: RunningMode, delegate: Delegate): PoseLandmarker {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelAssetPath).setDelegate(delegate).build())
            .setRunningMode(mode)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
            .setMinPosePresenceConfidence(MIN_CONFIDENCE)
            .setMinTrackingConfidence(MIN_CONFIDENCE)
            .build()
        return PoseLandmarker.createFromOptions(context, options).also {
            activeDelegate = delegate
            Log.i(TAG, "$mode landmarker: $modelAssetPath on $delegate")
        }
    }

    companion object {
        const val MODEL_ASSET = "pose_landmarker_full.task"
        private const val TAG = "PoseDetector"
        private const val MIN_CONFIDENCE = 0.5f
    }
}
