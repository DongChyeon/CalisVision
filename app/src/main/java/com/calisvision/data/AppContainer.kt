package com.calisvision.data

import com.calisvision.pose.VideoAnalyzer
import com.calisvision.video.VideoSource

interface AppContainer {
    val videoSource: VideoSource
    val analyzer: VideoAnalyzer
}
