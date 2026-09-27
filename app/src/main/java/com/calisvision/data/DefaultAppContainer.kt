package com.calisvision.data

import android.content.Context
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.calisvision.video.DefaultVideoAnalyzer
import com.calisvision.video.VideoAnalyzer

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val analyzer: VideoAnalyzer by lazy { DefaultVideoAnalyzer(appContext) }

    override val sessions = AnalysisSessionStore()

    override val pickVideo: ActivityResultContract<PickVisualMediaRequest, Uri?> = ActivityResultContracts.PickVisualMedia()
}
