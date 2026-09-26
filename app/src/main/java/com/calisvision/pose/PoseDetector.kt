package com.calisvision.pose

import android.graphics.Bitmap
import com.calisvision.domain.model.Landmark
import java.io.Closeable

interface PoseDetector : Closeable {
    fun detectImage(bitmap: Bitmap, rotationDeg: Int): List<Landmark>?

    fun detectVideo(bitmap: Bitmap, timestampMs: Long, rotationDeg: Int): List<Landmark>?
}
