package com.calisvision.ui.analysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisvision.data.AnalysisSession
import com.calisvision.data.AnalysisSessionStore
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.VideoAnalyzer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object ResolvingOrientation : AnalysisUiState

    data class Processing(val done: Int, val total: Int) : AnalysisUiState

    data class Failed(val detail: String?) : AnalysisUiState

    data class Completed(val sessionId: String) : AnalysisUiState
}

/** Runs one analysis for the lifetime of the screen; leaving the screen or [cancel] stops it (the analyzer drops its cache). */
class AnalysisViewModel(
    private val analyzer: VideoAnalyzer,
    private val sessions: AnalysisSessionStore,
    private val uri: Uri,
    private val exercise: Exercise,
) : ViewModel() {

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.ResolvingOrientation)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val job: Job = viewModelScope.launch {
        analyzer.analyze(uri, exercise).collect { progress ->
            _state.value = when (progress) {
                AnalysisProgress.ResolvingOrientation -> AnalysisUiState.ResolvingOrientation
                is AnalysisProgress.Processing -> AnalysisUiState.Processing(progress.done, progress.total)
                is AnalysisProgress.Failed -> AnalysisUiState.Failed(progress.error.message)
                is AnalysisProgress.Completed -> {
                    sessions.put(AnalysisSession(progress.result, exercise))
                    AnalysisUiState.Completed(progress.result.sessionId)
                }
            }
        }
    }

    fun cancel() {
        job.cancel()
    }
}
