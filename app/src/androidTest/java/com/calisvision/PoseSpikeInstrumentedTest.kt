package com.calisvision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.calisvision.domain.analysis.AlignmentGate
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.geometry.BodyOrientation
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Landmark
import com.calisvision.pose.MediaPipePoseDetector
import com.calisvision.pose.OrientationResolver
import com.calisvision.pose.PoseDetector
import com.calisvision.pose.PoseLandmarkerEngine
import com.calisvision.test.RequiresVideo
import com.calisvision.test.TestVideos
import com.calisvision.video.FrameSource
import com.calisvision.video.RetrieverFrameSource
import com.calisvision.video.SampledFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

/**
 * P1.5 De-risk spike: measures inverted-pose recognition on the real device with the production pipeline and writes
 * `getExternalFilesDir("spike")/spike_report.json` plus PoseFixture JSON to `getExternalFilesDir("fixtures")`.
 * Measurement only — the gate verdict is recorded in docs/VERIFICATION.md, so nothing here asserts it.
 *
 * Expected labels (manual): in the original tune video the body front faces image +x and shoulder→ankle points up,
 * so cross(ankle − shoulder, front) = cross((0, −1), (1, 0)) = 0·0 − (−1)·1 = +1. A 180° rotation maps every
 * difference vector v → −v, and cross(−u, −v) = cross(u, v), so the rot180 copy also expects frontSign = +1.
 * Orientation: original (hands at bottom) → 180°; rot180 copy (person already upright) → 0°.
 */
@RequiresVideo
@RunWith(AndroidJUnit4::class)
class PoseSpikeInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val exercise = HandstandKnowledge.exercise

    @Test
    fun spike(): Unit = runBlocking {
        val dir = TestVideos.dir(context)
        val tune = File(dir, "$TUNE.mp4")
        val rot180 = File(dir, "${TUNE}_rot180.mp4")
        assumeTrue("tune videos missing in $dir", tune.exists() && rot180.exists())

        val report = JSONObject()
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            .put("rotatedCoordsAreInRotatedFrameFlag", MediaPipePoseDetector.ROTATED_COORDS_ARE_IN_ROTATED_FRAME)

        val videos = JSONObject()
        val tuneRun = analyze(tune, expectedRotation = 180, withTheta = true)
        videos.put(TUNE, tuneRun.json)
        exportFixture(TUNE, tuneRun)
        val rotRun = analyze(rot180, expectedRotation = 0, withTheta = false)
        videos.put("${TUNE}_rot180", rotRun.json)
        rotRun.json.put("sideMatchesOriginal", rotRun.result.orientation?.side == tuneRun.result.orientation?.side)

        val holdout = File(dir, "$HOLDOUT.mp4")
        if (holdout.exists()) {
            // Reserved for P4: export the fixture only; angles are neither computed nor inspected here.
            val run = analyze(holdout, expectedRotation = null, withTheta = false)
            exportFixture(HOLDOUT, run)
            videos.put(HOLDOUT, JSONObject().put("fixtureExported", true).put("frameCount", run.result.frames.size))
        }
        report.put("videos", videos)

        File(dir, "${TUNE}_meta90.mp4").takeIf { it.exists() }?.let { report.put("a_rotationMetadata", probeRotation(tune, it)) }
        val hold = tuneRun.result.holdSegment ?: (tuneRun.result.frames.size / 4 until tuneRun.result.frames.size * 3 / 4)
        report.put("b_coordinateFrame", probeCoordinateFrame(tune, rot180, hold))

        val out = File(context.getExternalFilesDir("spike"), "spike_report.json")
        out.writeText(report.toString(2))
        Log.i(TAG, "report written to $out\n${report.toString(2)}")
    }

    private class Run(val result: AnalysisResult, val json: JSONObject)

    private suspend fun analyze(file: File, expectedRotation: Int?, withTheta: Boolean): Run {
        val uri = Uri.fromFile(file)
        val decodeMs = mutableListOf<Double>()
        val inferMs = mutableListOf<Double>()
        var totalMs = 0.0
        lateinit var scores: Triple<Float, Float, Float>
        val (rotation, processed) = RetrieverFrameSource.open(context, uri).use { raw ->
            val source = TimedFrameSource(raw, decodeMs)
            val rotation = MediaPipePoseDetector(context).use { detector ->
                scores = orientationScores(raw, detector)
                // Total analysis time = production resolve + process (the extra scoring pass above is excluded).
                val start = System.nanoTime()
                OrientationResolver.resolve(raw, exercise, detector).also { totalMs += elapsedMs(start) }
            }
            val start = System.nanoTime()
            val processed = PoseLandmarkerEngine { TimedDetector(MediaPipePoseDetector(context), inferMs) }
                .process(source, rotation, frameDir = null) { _, _ -> }
            totalMs += elapsedMs(start)
            rotation to processed
        }
        val result = AnalysisResult.assemble(
            sessionId = file.nameWithoutExtension,
            videoUri = uri.toString(),
            width = processed.width,
            height = processed.height,
            rotationDegrees = rotation,
            frames = processed.poses,
            exercise = exercise,
            frameDir = "",
        )
        val hold = result.holdSegment
        val holdFrames = hold?.let { r -> result.frames.filter { it.sampleIndex in r } } ?: emptyList()
        val orientation = result.orientation

        val json = JSONObject()
            .put("frameSize", "${processed.width}x${processed.height}")
            .put("sampledFrames", result.frames.size)
            .put("detectionRate", detectionRate(result.frames))
            .put("holdSegment", hold?.let { "${it.first}..${it.last}" } ?: JSONObject.NULL)
            .put("holdSamples", holdFrames.size)
            .put("holdDetectionRate", if (holdFrames.isEmpty()) JSONObject.NULL else detectionRate(holdFrames))
            .put(
                "orientation",
                JSONObject()
                    .put("score0", scores.first.toDouble())
                    .put("score180", scores.second.toDouble())
                    .put("score180ManualBitmapRotation", scores.third.toDouble())
                    .put("chosen", rotation)
                    .put("expected", expectedRotation ?: JSONObject.NULL)
                    .put("correct", expectedRotation?.let { it == rotation } ?: JSONObject.NULL),
            )
            .put(
                "sideSelector",
                JSONObject()
                    .put("side", orientation?.side?.name ?: JSONObject.NULL)
                    .put("frontSign", orientation?.frontSign ?: JSONObject.NULL)
                    .put("expectedFrontSign", if (expectedRotation != null) EXPECTED_FRONT_SIGN else JSONObject.NULL)
                    .put("correct", if (expectedRotation != null) orientation?.frontSign == EXPECTED_FRONT_SIGN else JSONObject.NULL),
            )
            .put(
                "timing",
                JSONObject()
                    .put("decodeMsPerFrame", decodeMs.average())
                    .put("inferenceMsPerFrame", inferMs.average())
                    .put("decodeFrames", decodeMs.size)
                    .put("inferenceFrames", inferMs.size)
                    .put("totalAnalysisMs", totalMs),
            )
        if (orientation != null) {
            json.put("meanVisibility", visibility(result.frames, orientation))
            if (holdFrames.isNotEmpty()) json.put("holdMeanVisibility", visibility(holdFrames, orientation))
            if (withTheta && hold != null) json.put("holdTheta", thetaStats(result, orientation, hold))
        }
        return Run(result, json)
    }

    /**
     * Production scores at 0°/180° (setRotationDegrees), plus the score when the bitmap is rotated 180° in memory, detected at 0°
     * and mapped back with (1 − x, 1 − y) — shows what the resolver would pick if rotation bypassed setRotationDegrees.
     */
    private fun orientationScores(source: FrameSource, detector: PoseDetector): Triple<Float, Float, Float> {
        val upright = mutableListOf<FramePose>()
        val inverted = mutableListOf<FramePose>()
        val manual = mutableListOf<FramePose>()
        for (index in OrientationResolver.sampleIndices(source.info.frameCount)) {
            val frame = source.frameAt(index) ?: continue
            upright += FramePose(index, frame.displayTimeMs, detector.detectImage(frame.bitmap, 0))
            inverted += FramePose(index, frame.displayTimeMs, detector.detectImage(frame.bitmap, 180))
            val rotated = Bitmap.createBitmap(frame.bitmap, 0, 0, frame.bitmap.width, frame.bitmap.height, Matrix().apply { postRotate(180f) }, true)
            val back = detector.detectImage(rotated, 0)?.map { it.copy(x = 1f - it.x, y = 1f - it.y) }
            manual += FramePose(index, frame.displayTimeMs, back)
            rotated.recycle()
            frame.bitmap.recycle()
        }
        return Triple(
            OrientationResolver.score(upright, exercise),
            OrientationResolver.score(inverted, exercise),
            OrientationResolver.score(manual, exercise),
        )
    }

    private fun detectionRate(frames: List<FramePose>) = frames.count { it.landmarks != null }.toDouble() / frames.size

    /** Mean visibility of the 4-rule joints on the chosen side over detected frames (undetected frames excluded). */
    private fun visibility(frames: List<FramePose>, orientation: BodyOrientation): JSONObject {
        val detected = frames.filter { it.landmarks != null }
        val json = JSONObject()
        val perJoint = exercise.keyJoints.map { joint ->
            val mean = detected.map { (it[orientation.side, joint]?.visibility ?: 0f).toDouble() }.average()
            json.put(joint.name, mean)
            mean
        }
        return json.put("mean", perJoint.average())
    }

    private fun thetaStats(result: AnalysisResult, orientation: BodyOrientation, hold: IntRange): JSONObject {
        val raw = AngleTimeline.build(result.frames, exercise, orientation, result.aspect)
        val json = JSONObject()
        for (rule in exercise.rules) {
            val rawSeries = raw.frames.filter { it.sampleIndex in hold }.mapNotNull { it.angles[rule.id] }
            val smoothed = result.timeline.frames.filter { it.sampleIndex in hold }.map { it.angles[rule.id] }
            val valid = smoothed.filterNotNull()
            val stats = JSONObject()
                .put("mean", if (valid.isEmpty()) JSONObject.NULL else valid.average())
                .put("min", valid.minOrNull()?.toDouble() ?: JSONObject.NULL)
                .put("max", valid.maxOrNull()?.toDouble() ?: JSONObject.NULL)
                .put("validSmoothed", valid.size)
                .put("rawMean", if (rawSeries.isEmpty()) JSONObject.NULL else rawSeries.average())
            if (rule.id == HandstandKnowledge.ALIGNMENT) {
                val aligned = smoothed.count { it != null && it in AlignmentGate.ALIGNED }
                stats.put("pctIn175to185", aligned.toDouble() / smoothed.size)
            }
            json.put(rule.id.value, stats)
        }
        return json
    }

    private fun exportFixture(name: String, run: Run) {
        val frames = JSONArray()
        for (pose in run.result.frames) {
            val landmarks = pose.landmarks?.let { list ->
                JSONArray().apply { list.forEach { put(landmarkJson(it)) } }
            }
            frames.put(
                JSONObject()
                    .put("sampleIndex", pose.sampleIndex)
                    .put("displayTimeMs", pose.displayTimeMs)
                    .put("landmarks", landmarks ?: JSONObject.NULL),
            )
        }
        val json = JSONObject().put("width", run.result.width).put("height", run.result.height).put("frames", frames)
        File(context.getExternalFilesDir("fixtures"), "$name.json").writeText(json.toString())
    }

    private fun landmarkJson(l: Landmark) = JSONObject()
        .put("x", l.x.toDouble())
        .put("y", l.y.toDouble())
        .put("z", l.z.toDouble())
        .put("visibility", l.visibility.toDouble())

    /** (a): does getFrameAtTime apply the container's rotation metadata to the decoded bitmap? */
    private fun probeRotation(original: File, meta90: File): JSONObject {
        fun probe(file: File): JSONObject = RetrieverFrameSource.open(context, Uri.fromFile(file)).use { source ->
            val frame = source.frameAt(source.info.frameCount / 2)
            val full = MediaMetadataRetriever().run {
                try {
                    setDataSource(file.absolutePath)
                    getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                } finally {
                    release()
                }
            }
            JSONObject()
                .put("metadataRotation", source.info.rotationDegrees)
                .put("metadataSize", "${source.info.width}x${source.info.height}")
                .put("fullBitmap", full?.let { "${it.width}x${it.height}" } ?: JSONObject.NULL)
                .put("frameSourceBitmap", frame?.let { "${it.bitmap.width}x${it.bitmap.height}" } ?: JSONObject.NULL)
                .also {
                    full?.recycle()
                    frame?.bitmap?.recycle()
                }
        }
        val orig = probe(original)
        val meta = probe(meta90)
        val autoApplied = orig.getString("fullBitmap") != meta.getString("fullBitmap")
        return JSONObject().put("original", orig).put("meta90", meta).put("rotationAutoApplied", autoApplied)
    }

    /**
     * (b): raw MediaPipe landmarks (no back-transform) of the ORIGINAL frame run at rotation 180 vs the rot180 copy's frame
     * at rotation 0 (L). raw ≈ L → coordinates are in the rotated frame; raw ≈ (1 − x, 1 − y) of L → the input frame.
     * The whole hold segment is scanned until [PROBE_FRAMES] frames detect at 180°. Control: the original bitmap rotated
     * 180° in memory (Matrix) and detected at 0°, compared with L, isolates setRotationDegrees from the re-encoded copy.
     */
    private fun probeCoordinateFrame(original: File, rot180: File, hold: IntRange): JSONObject {
        val sameDiffs = mutableListOf<Double>()
        val flipDiffs = mutableListOf<Double>()
        val prodDiffs = mutableListOf<Double>()
        val controlDiffs = mutableListOf<Double>()
        var scanned = 0
        var detectedAt180 = 0
        var controlDetected = 0
        var controlTried = 0
        val perFrame = JSONArray()
        rawLandmarker().use { landmarker ->
            MediaPipePoseDetector(context).use { detector ->
                RetrieverFrameSource.open(context, Uri.fromFile(original)).use { origSource ->
                    RetrieverFrameSource.open(context, Uri.fromFile(rot180)).use { rotSource ->
                        for (index in hold) {
                            if (sameDiffs.size >= PROBE_FRAMES) break
                            val o = origSource.frameAt(index) ?: continue
                            scanned++
                            val raw = rawDetect(landmarker, o.bitmap, 180)
                            val needControl = controlTried < PROBE_FRAMES
                            if (raw == null && !needControl) {
                                o.bitmap.recycle()
                                continue
                            }
                            val r = rotSource.frameAt(index)
                            val reference = r?.let { rawDetect(landmarker, it.bitmap, 0) }
                            r?.bitmap?.recycle()
                            if (needControl) {
                                controlTried++
                                val rotated = Bitmap.createBitmap(o.bitmap, 0, 0, o.bitmap.width, o.bitmap.height, Matrix().apply { postRotate(180f) }, true)
                                val control = rawDetect(landmarker, rotated, 0)
                                rotated.recycle()
                                if (control != null) controlDetected++
                                if (control != null && reference != null) controlDiffs += meanAbsDiff(control, reference, flip = false)
                            }
                            if (raw == null || reference == null) {
                                o.bitmap.recycle()
                                continue
                            }
                            detectedAt180++
                            val production = detector.detectImage(o.bitmap, 180)
                            o.bitmap.recycle()
                            val same = meanAbsDiff(raw, reference, flip = false)
                            val flip = meanAbsDiff(raw, reference, flip = true)
                            sameDiffs += same
                            flipDiffs += flip
                            // Production output is in original-frame coords, so it should match (1 − x, 1 − y) of L.
                            production?.let { p -> prodDiffs += meanAbsDiff(p.map { it.x to it.y }, reference, flip = true) }
                            perFrame.put(JSONObject().put("sampleIndex", index).put("sameDiff", same).put("flipDiff", flip))
                        }
                    }
                }
            }
        }
        fun avg(values: List<Double>): Any = if (values.isEmpty()) JSONObject.NULL else values.average()
        return JSONObject()
            .put("holdFramesScanned", scanned)
            .put("originalAt180DetectedAndCompared", detectedAt180)
            .put("framesCompared", sameDiffs.size)
            .put("meanDiffRawVsL", avg(sameDiffs))
            .put("meanDiffRawVsFlippedL", avg(flipDiffs))
            .put("coordsInRotatedFrame", if (sameDiffs.isEmpty()) JSONObject.NULL else sameDiffs.average() < flipDiffs.average())
            .put("productionBackTransformDiffVsFlippedL", avg(prodDiffs))
            .put("controlMatrixRotatedAt0Detected", "$controlDetected/$controlTried")
            .put("controlMatrixRotatedVsL", avg(controlDiffs))
            .put("perFrame", perFrame)
    }

    private fun rawLandmarker(): PoseLandmarker = PoseLandmarker.createFromOptions(
        context,
        PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MediaPipePoseDetector.MODEL_ASSET).build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build(),
    )

    private fun rawDetect(landmarker: PoseLandmarker, bitmap: Bitmap, rotation: Int): List<Pair<Float, Float>>? =
        landmarker.detect(BitmapImageBuilder(bitmap).build(), ImageProcessingOptions.builder().setRotationDegrees(rotation).build())
            .landmarks().firstOrNull()?.map { it.x() to it.y() }

    private fun meanAbsDiff(a: List<Pair<Float, Float>>, b: List<Pair<Float, Float>>, flip: Boolean): Double =
        a.zip(b).map { (p, q) ->
            val qx = if (flip) 1f - q.first else q.first
            val qy = if (flip) 1f - q.second else q.second
            (abs(p.first - qx) + abs(p.second - qy)) / 2.0
        }.average()

    private class TimedFrameSource(private val delegate: FrameSource, private val ms: MutableList<Double>) : FrameSource {
        override val info = delegate.info

        override fun frameAt(sampleIndex: Int): SampledFrame? {
            val start = System.nanoTime()
            return delegate.frameAt(sampleIndex).also { ms += elapsedMs(start) }
        }

        override fun close() = delegate.close()
    }

    private class TimedDetector(private val delegate: PoseDetector, private val ms: MutableList<Double>) : PoseDetector by delegate {
        override fun detectVideo(bitmap: Bitmap, timestampMs: Long, rotationDeg: Int): List<Landmark>? {
            val start = System.nanoTime()
            return delegate.detectVideo(bitmap, timestampMs, rotationDeg).also { ms += elapsedMs(start) }
        }
    }

    private companion object {
        fun elapsedMs(startNanos: Long) = (System.nanoTime() - startNanos) / 1e6

        const val TAG = "PoseSpike"
        const val TUNE = "wall_handstand_tune"
        const val HOLDOUT = "wall_handstand_holdout"
        const val EXPECTED_FRONT_SIGN = 1
        const val PROBE_FRAMES = 5
    }
}
