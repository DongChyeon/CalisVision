package com.calisvision

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.model.Landmark
import com.calisvision.pose.MediaPipePoseDetector
import com.calisvision.pose.OrientationResolver
import com.calisvision.pose.PoseDetector
import com.calisvision.pose.PoseLandmarkerEngine
import com.calisvision.test.RequiresVideo
import com.calisvision.test.TestVideos
import com.calisvision.video.CodecFrameSource
import com.google.mediapipe.tasks.core.Delegate
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * P4 model/delegate comparison ({full, heavy} × {CPU, GPU}) with the production pipeline (frame JPEGs not written).
 * heavy is read from `<external files>/models/pose_landmarker_heavy.task` (pushed for the test only; skipped when
 * absent). Writes `<external files>/spike/model_compare.json`; measurement only, nothing is asserted.
 */
@RequiresVideo
@RunWith(AndroidJUnit4::class)
class ModelDelegateBenchmarkTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val exercise = HandstandKnowledge.exercise

    private data class Config(val name: String, val model: String, val delegate: Delegate)

    @Test
    fun compare(): Unit = runBlocking {
        val dir = TestVideos.dir(context)
        val tune = File(dir, "$TUNE.mp4")
        val long = File(dir, "${TUNE}_30s.mp4")
        assumeTrue("videos missing in $dir", tune.exists() && long.exists())
        val heavy = File(context.getExternalFilesDir("models"), HEAVY)
        val configs = listOf(
            Config("full-CPU", MediaPipePoseDetector.MODEL_ASSET, Delegate.CPU),
            Config("full-GPU", MediaPipePoseDetector.MODEL_ASSET, Delegate.GPU),
        ) + if (heavy.exists()) {
            listOf(Config("heavy-CPU", heavy.absolutePath, Delegate.CPU), Config("heavy-GPU", heavy.absolutePath, Delegate.GPU))
        } else {
            emptyList()
        }

        val report = JSONObject()
        lateinit var reference: AnalysisResult
        for (config in configs) {
            Thread.sleep(COOL_DOWN_MS)
            val tuneRun = analyze(tune, config)
            if (config == configs.first()) reference = tuneRun.result
            tuneRun.json.put("meanAbsDeltaThetaVsFullCpuOverTuneHold", deltaTheta(tuneRun.result, reference))
            val longRun = analyze(long, config)
            report.put(config.name, JSONObject().put("tune", tuneRun.json).put("tune_30s", longRun.json))
            write(report, "model_compare.json")
        }
    }

    /** Three back-to-back 30 s runs per delegate on the full model (thermal slowdown). */
    @Test
    fun thermal30s(): Unit = runBlocking {
        val long = File(TestVideos.dir(context), "${TUNE}_30s.mp4")
        assumeTrue("30 s video missing", long.exists())
        val report = JSONObject()
        for (delegate in listOf(Delegate.CPU, Delegate.GPU)) {
            Thread.sleep(COOL_DOWN_MS)
            val config = Config("full-$delegate", MediaPipePoseDetector.MODEL_ASSET, delegate)
            val runs = JSONArray()
            repeat(3) { runs.put(analyze(long, config).json) }
            report.put(config.name, runs)
            write(report, "thermal_30s.json")
        }
    }

    private class Run(val result: AnalysisResult, val json: JSONObject)

    private suspend fun analyze(file: File, config: Config): Run {
        val uri = Uri.fromFile(file)
        val inferMs = mutableListOf<Double>()
        var active: Delegate? = null
        val start = System.nanoTime()
        var resolveMs = 0.0
        val (rotation, processed) = CodecFrameSource.open(context, uri).use { source ->
            val rotation = MediaPipePoseDetector(context, config.model, config.delegate).use { detector ->
                OrientationResolver.resolve(source, exercise, detector).also { active = detector.activeDelegate }
            }
            resolveMs = elapsedMs(start)
            val processed = PoseLandmarkerEngine {
                TimedDetector(MediaPipePoseDetector(context, config.model, config.delegate), inferMs)
            }.process(source, rotation, frameDir = null) { _, _ -> }
            rotation to processed
        }
        val totalMs = elapsedMs(start)
        val result = AnalysisResult.assemble(
            file.nameWithoutExtension, uri.toString(), processed.width, processed.height, rotation, processed.poses, exercise, "",
        )
        val detected = result.frames.filter { it.landmarks != null }
        val side = result.orientation?.side
        val visibility = if (side == null || detected.isEmpty()) JSONObject.NULL else exercise.keyJoints
            .map { joint -> detected.map { (it[side, joint]?.visibility ?: 0f).toDouble() }.average() }
            .average()
        val json = JSONObject()
            .put("config", config.name)
            .put("resolveDelegate", active?.name ?: JSONObject.NULL)
            .put("samples", result.frames.size)
            .put("detectionRate", detected.size.toDouble() / result.frames.size)
            .put("meanVisibility", visibility)
            .put("rotation", rotation)
            .put("orientation", result.orientation?.toString() ?: JSONObject.NULL)
            .put("holdSegment", result.holdSegment?.let { "${it.first}..${it.last}" } ?: JSONObject.NULL)
            .put("inferenceMsPerFrame", inferMs.average())
            .put("resolveMs", resolveMs)
            .put("totalAnalysisMs", totalMs)
        Log.i(TAG, "${file.name}: $json")
        return Run(result, json)
    }

    /** Per rule, mean |θ − θ_ref| of the smoothed series over the reference hold (samples where both are non-null). */
    private fun deltaTheta(result: AnalysisResult, reference: AnalysisResult): JSONObject {
        val json = JSONObject()
        val hold = reference.holdSegment ?: return json
        val ref = reference.timeline.frames.associateBy { it.sampleIndex }
        for (rule in exercise.rules) {
            val deltas = result.timeline.frames.filter { it.sampleIndex in hold }.mapNotNull { frame ->
                val a = frame.angles[rule.id] ?: return@mapNotNull null
                val b = ref[frame.sampleIndex]?.angles?.get(rule.id) ?: return@mapNotNull null
                abs(a - b).toDouble()
            }
            json.put(rule.id.value, if (deltas.isEmpty()) JSONObject.NULL else deltas.average())
        }
        return json
    }

    private fun write(report: JSONObject, name: String) {
        File(context.getExternalFilesDir("spike"), name).writeText(report.toString(2))
    }

    private class TimedDetector(private val delegate: PoseDetector, private val ms: MutableList<Double>) : PoseDetector by delegate {
        override fun detectVideo(bitmap: Bitmap, timestampMs: Long, rotationDeg: Int): List<Landmark>? {
            val start = System.nanoTime()
            return delegate.detectVideo(bitmap, timestampMs, rotationDeg).also { ms += elapsedMs(start) }
        }
    }

    private companion object {
        fun elapsedMs(startNanos: Long) = (System.nanoTime() - startNanos) / 1e6

        const val TAG = "ModelBenchmark"
        const val TUNE = "wall_handstand_tune"
        const val HEAVY = "pose_landmarker_heavy.task"
        const val COOL_DOWN_MS = 30_000L
    }
}
