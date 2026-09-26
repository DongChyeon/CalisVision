package com.calisvision.test

import android.content.Context
import java.io.File

/** /sdcard/Android/data/com.calisvision/files/test-videos/ */
object TestVideos {
    fun dir(context: Context): File? = context.getExternalFilesDir("test-videos")
}
