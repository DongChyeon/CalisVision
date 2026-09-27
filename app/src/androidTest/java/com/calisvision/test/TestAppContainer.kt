package com.calisvision.test

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import com.calisvision.data.AnalysisSessionStore
import com.calisvision.data.AppContainer
import com.calisvision.data.InMemoryThresholdRepository
import com.calisvision.data.ThresholdRepository
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.VideoAnalyzer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TestAppContainer(
    override val analyzer: VideoAnalyzer = FakeAnalyzer(),
    override val pickVideo: FakePickVideo = FakePickVideo(),
    override val sessions: AnalysisSessionStore = AnalysisSessionStore(),
    override val thresholds: ThresholdRepository = InMemoryThresholdRepository(),
) : AppContainer

/** Returns [result] synchronously instead of opening the system picker; [launches] counts picker opens. */
class FakePickVideo(var result: Uri? = Uri.parse("content://com.calisvision.test/video.mp4")) :
    ActivityResultContract<PickVisualMediaRequest, Uri?>() {
    var launches = 0
        private set

    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent = error("picker must not start an activity in tests")

    override fun getSynchronousResult(context: Context, input: PickVisualMediaRequest): SynchronousResult<Uri?> {
        launches++
        return SynchronousResult(result)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = null
}

/** Emits [progress]; when [gate] is set, suspends before [AnalysisProgress.Completed] until the gate completes. */
class FakeAnalyzer(
    private val gate: CompletableDeferred<Unit>? = null,
    private val progress: (Uri) -> List<AnalysisProgress> = { listOf(AnalysisProgress.Completed(emptyResult(it))) },
) : VideoAnalyzer {
    override fun analyze(uri: Uri, exercise: Exercise): Flow<AnalysisProgress> = flow {
        progress(uri).forEach {
            if (it is AnalysisProgress.Completed) gate?.await()
            emit(it)
        }
    }

    companion object {
        fun emptyResult(uri: Uri) = AnalysisResult(
            sessionId = "test",
            videoUri = uri.toString(),
            width = 0,
            height = 0,
            rotationDegrees = 0,
            frames = emptyList(),
            orientation = null,
            holdSegment = null,
            timeline = AngleTimeline(emptyList()),
            faults = emptyList(),
            frameDir = "",
        )
    }
}
