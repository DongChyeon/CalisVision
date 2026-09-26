package com.calisvision.data

import android.content.Context
import com.calisvision.pose.MediaPipeVideoAnalyzer
import com.calisvision.pose.VideoAnalyzer
import com.calisvision.video.MediaRetrieverVideoSource
import com.calisvision.video.VideoSource

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val videoSource: VideoSource by lazy { MediaRetrieverVideoSource(appContext) }
    override val analyzer: VideoAnalyzer by lazy { MediaPipeVideoAnalyzer(appContext) }
}
