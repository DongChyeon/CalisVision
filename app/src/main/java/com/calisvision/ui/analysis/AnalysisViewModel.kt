package com.calisvision.ui.analysis

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisvision.data.AnalysisSession
import com.calisvision.data.AnalysisSessionStore
import com.calisvision.domain.rules.Exercise
import com.calisvision.video.AnalysisProgress
import com.calisvision.video.VideoAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

sealed interface AnalysisUiState {
    data object ResolvingOrientation : AnalysisUiState

    /** [remainingSeconds] is null until [MIN_SAMPLES_FOR_ESTIMATE] samples are done. */
    data class Processing(val done: Int, val total: Int, val remainingSeconds: Int? = null) : AnalysisUiState

    data class Failed(val detail: String?) : AnalysisUiState

    data class Completed(val sessionId: String) : AnalysisUiState
}

const val MIN_SAMPLES_FOR_ESTIMATE = 5

/**
 * Seconds left at the pace so far: [elapsedMs] was spent on the samples after [baseline] up to [done].
 * Null before [MIN_SAMPLES_FOR_ESTIMATE] samples are done or while no pace is measurable.
 */
fun estimateRemainingSeconds(elapsedMs: Long, done: Int, total: Int, baseline: Int = 0): Int? {
    val measured = done - baseline
    if (done < MIN_SAMPLES_FOR_ESTIMATE || measured <= 0 || elapsedMs <= 0) return null
    val remainingMs = elapsedMs.toDouble() * (total - done).coerceAtLeast(0) / measured
    return ceil(remainingMs / 1000).toInt()
}

/** OpenableColumns.DISPLAY_NAME of [uri] read off the main thread; null when the provider has none. */
suspend fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}

/**
 * Runs one analysis for the lifetime of the screen; leaving the screen or [cancel] stops it (the analyzer drops its cache).
 * [fileName] is the picked video's display name once [displayName] resolves it; [now] is a monotonic clock in ms.
 */
class AnalysisViewModel(
    private val analyzer: VideoAnalyzer,
    private val sessions: AnalysisSessionStore,
    private val uri: Uri,
    private val exercise: Exercise,
    displayName: suspend () -> String? = { null },
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
) : ViewModel() {

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.ResolvingOrientation)
    val state: StateFlow<AnalysisUiState> = _state.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)
    val fileName: StateFlow<String?> = _fileName.asStateFlow()

    /** Time and done count of the first Processing update; the pace is measured from there. */
    private var processingStart: Pair<Long, Int>? = null

    init {
        viewModelScope.launch { _fileName.value = displayName() }
    }

    private val job: Job = viewModelScope.launch {
        analyzer.analyze(uri, exercise).collect { progress ->
            _state.value = when (progress) {
                AnalysisProgress.ResolvingOrientation -> AnalysisUiState.ResolvingOrientation
                is AnalysisProgress.Processing -> {
                    val (startMs, baseline) = processingStart ?: (now() to progress.done).also { processingStart = it }
                    val remaining = estimateRemainingSeconds(now() - startMs, progress.done, progress.total, baseline)
                    AnalysisUiState.Processing(progress.done, progress.total, remaining)
                }
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
