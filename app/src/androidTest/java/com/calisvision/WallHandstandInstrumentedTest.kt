package com.calisvision

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.test.RequiresVideo
import com.calisvision.test.TestVideos
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.DefaultVideoAnalyzer
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * P4 (AC-4): runs the production analyzer (CodecFrameSource → OrientationResolver → PoseLandmarkerEngine →
 * AnalysisResult.assemble) on the reference videos and writes PoseFixture JSON to `getExternalFilesDir("fixtures")`
 * for `adb pull` into `app/src/test/resources/fixtures/`. The AC-4 verdict is `WallHandstandAlignmentTest` (JVM).
 */
@RequiresVideo
@RunWith(AndroidJUnit4::class)
class WallHandstandInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun exportHoldoutFixture() = export(HOLDOUT)

    @Test
    fun exportTuneFixture() = export(TUNE)

    private fun export(name: String): Unit = runBlocking {
        val video = File(TestVideos.dir(context), "$name.mp4")
        assumeTrue("video missing: $video", video.exists())
        val start = System.nanoTime()
        val result = when (val end = DefaultVideoAnalyzer(context).analyze(Uri.fromFile(video)).filter { it.isTerminal() }.first()) {
            is AnalysisProgress.Completed -> end.result
            is AnalysisProgress.Failed -> throw end.error
            else -> error("unexpected $end")
        }
        val totalMs = (System.nanoTime() - start) / 1_000_000
        File(result.frameDir).deleteRecursively()
        val out = File(context.getExternalFilesDir("fixtures"), "$name.json")
        out.writeText(fixtureJson(result).toString())
        Log.i(TAG, "$name: ${result.frames.size} samples, rotation ${result.rotationDegrees}, hold ${result.holdSegment}, ${totalMs}ms → $out")
        if (result.frames.none { it.landmarks != null }) fail("$name: no pose detected")
    }

    private fun AnalysisProgress.isTerminal() = this is AnalysisProgress.Completed || this is AnalysisProgress.Failed

    private fun fixtureJson(result: AnalysisResult): JSONObject {
        val frames = JSONArray()
        for (pose in result.frames) {
            val landmarks = pose.landmarks?.let { list ->
                JSONArray().apply {
                    list.forEach {
                        put(
                            JSONObject()
                                .put("x", it.x.toDouble())
                                .put("y", it.y.toDouble())
                                .put("z", it.z.toDouble())
                                .put("visibility", it.visibility.toDouble()),
                        )
                    }
                }
            }
            frames.put(
                JSONObject()
                    .put("sampleIndex", pose.sampleIndex)
                    .put("displayTimeMs", pose.displayTimeMs)
                    .put("landmarks", landmarks ?: JSONObject.NULL),
            )
        }
        return JSONObject().put("width", result.width).put("height", result.height).put("frames", frames)
    }

    private companion object {
        const val TAG = "WallHandstand"
        const val TUNE = "wall_handstand_tune"
        const val HOLDOUT = "wall_handstand_holdout"
    }
}
