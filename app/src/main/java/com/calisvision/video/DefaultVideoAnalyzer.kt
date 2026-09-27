package com.calisvision.video

import android.content.Context
import android.net.Uri
import com.calisvision.CalisVisionApp
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.rules.Exercise
import com.calisvision.pose.MediaPipePoseDetector
import com.calisvision.pose.OrientationResolver
import com.calisvision.pose.PoseDetector
import com.calisvision.pose.PoseLandmarkerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

class DefaultVideoAnalyzer(
    private val context: Context,
    private val detectorFactory: () -> PoseDetector = { MediaPipePoseDetector(context) },
) : VideoAnalyzer {

    override fun analyze(uri: Uri, exercise: Exercise): Flow<AnalysisProgress> = flow {
        emit(AnalysisProgress.ResolvingOrientation)
        val sessionId = UUID.randomUUID().toString()
        val frameDir = File(context.cacheDir, "${CalisVisionApp.ANALYSIS_DIR}/$sessionId")
        val result = try {
            CodecFrameSource.open(context, uri).use { source ->
                val rotation = detectorFactory().use { OrientationResolver.resolve(source, exercise, it) }
                val processed = PoseLandmarkerEngine(detectorFactory).process(source, rotation, frameDir) { done, total ->
                    emit(AnalysisProgress.Processing(done, total))
                }
                AnalysisResult.assemble(
                    sessionId = sessionId,
                    videoUri = uri.toString(),
                    width = processed.width,
                    height = processed.height,
                    rotationDegrees = rotation,
                    frames = processed.poses,
                    exercise = exercise,
                    frameDir = frameDir.absolutePath,
                )
            }
        } catch (e: Throwable) {
            frameDir.deleteRecursively()
            throw e
        }
        emit(AnalysisProgress.Completed(result))
    }
        .catch { emit(AnalysisProgress.Failed(it)) }
        .flowOn(Dispatchers.Default)
}
