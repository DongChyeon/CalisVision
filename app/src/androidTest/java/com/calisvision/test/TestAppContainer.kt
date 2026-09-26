package com.calisvision.test

import android.net.Uri
import com.calisvision.data.AppContainer
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.pose.VideoAnalyzer
import com.calisvision.video.VideoHandle
import com.calisvision.video.VideoSource

class TestAppContainer(
    override val videoSource: VideoSource = FakeVideoSource(),
    override val analyzer: VideoAnalyzer = FakeAnalyzer(),
) : AppContainer

class FakeVideoSource : VideoSource {
    override suspend fun openVideo(uri: Uri): VideoHandle =
        VideoHandle(uri = uri, durationMs = 0L, width = 0, height = 0, rotationDegrees = 0)
}

class FakeAnalyzer(
    private val result: (Uri) -> AnalysisResult = { AnalysisResult(it.toString(), 0, 0) },
) : VideoAnalyzer {
    override suspend fun analyze(uri: Uri): AnalysisResult = result(uri)
}
