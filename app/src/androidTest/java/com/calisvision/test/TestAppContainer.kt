package com.calisvision.test

import android.net.Uri
import com.calisvision.data.AppContainer
import com.calisvision.domain.analysis.AngleTimeline
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.VideoAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class TestAppContainer(
    override val analyzer: VideoAnalyzer = FakeAnalyzer(),
) : AppContainer

class FakeAnalyzer(
    private val progress: (Uri) -> List<AnalysisProgress> = { listOf(AnalysisProgress.Completed(emptyResult(it))) },
) : VideoAnalyzer {
    override fun analyze(uri: Uri, exercise: Exercise): Flow<AnalysisProgress> = flowOf(*progress(uri).toTypedArray())

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
