package com.calisvision.pose

import android.net.Uri
import com.calisvision.domain.model.AnalysisResult

interface VideoAnalyzer {
    suspend fun analyze(uri: Uri): AnalysisResult
}
