package com.calisvision.video

import android.net.Uri
import com.calisvision.domain.knowledge.HandstandKnowledge
import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.rules.Exercise
import kotlinx.coroutines.flow.Flow

sealed interface AnalysisProgress {
    data object ResolvingOrientation : AnalysisProgress

    data class Processing(val done: Int, val total: Int) : AnalysisProgress

    data class Completed(val result: AnalysisResult) : AnalysisProgress

    data class Failed(val error: Throwable) : AnalysisProgress
}

interface VideoAnalyzer {
    fun analyze(uri: Uri, exercise: Exercise = HandstandKnowledge.exercise): Flow<AnalysisProgress>
}
