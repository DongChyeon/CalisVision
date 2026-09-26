package com.calisvision.data

import android.content.Context
import com.calisvision.video.DefaultVideoAnalyzer
import com.calisvision.video.VideoAnalyzer

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val analyzer: VideoAnalyzer by lazy { DefaultVideoAnalyzer(appContext) }
}
