package com.calisvision.data

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import com.calisvision.video.VideoAnalyzer

interface AppContainer {
    val analyzer: VideoAnalyzer

    /** Gallery picker; replaceable so instrumented tests can return a video without the system UI. */
    val pickVideo: ActivityResultContract<PickVisualMediaRequest, Uri?>
}
