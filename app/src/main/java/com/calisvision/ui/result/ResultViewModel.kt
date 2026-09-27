package com.calisvision.ui.result

import androidx.lifecycle.ViewModel
import com.calisvision.data.AnalysisSession
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.RuleId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/** Image, skeleton and angles of one sample; [faultJoints] are vertices of rules this sample breaks. */
data class FrameOverlay(val pose: FramePose?, val faultJoints: Set<Joint>)

data class ResultUiState(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val angles: Map<RuleId, Float?>,
    val faults: List<FaultSegment>,
)

/**
 * The scrubber position is a sampleIndex: frames hold one entry per 0.1 s sample in order (PoseLandmarkerEngine fills
 * index i with sample i), so one step is exactly one sample and image, skeleton and angles share it (AC-7).
 */
class ResultViewModel(val session: AnalysisSession) : ViewModel() {

    val result get() = session.result
    val sampleCount: Int get() = result.frames.size

    private val anglesBySample = result.timeline.frames.associate { it.sampleIndex to it.angles }
    private val thresholds: Map<RuleId, AngleThreshold> = session.exercise.rules.associate { it.id to it.threshold }

    private val _state = MutableStateFlow(stateAt(result.holdSegment?.first ?: 0))
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    fun seekTo(sampleIndex: Int) {
        if (sampleCount == 0) return
        _state.update { stateAt(sampleIndex.coerceIn(0, sampleCount - 1)) }
    }

    fun step(delta: Int) = seekTo(state.value.sampleIndex + delta)

    fun framePath(sampleIndex: Int): File = File(result.frameDir, "$sampleIndex.jpg")

    fun overlay(sampleIndex: Int): FrameOverlay {
        val angles = anglesBySample[sampleIndex].orEmpty()
        val faultJoints = session.exercise.rules
            .filter { rule -> rule.isBrokenBy(angles[rule.id], thresholds.getValue(rule.id)) }
            .mapTo(mutableSetOf()) { it.joints[1] }
        return FrameOverlay(result.frames.getOrNull(sampleIndex), faultJoints)
    }

    private fun stateAt(sampleIndex: Int) = ResultUiState(
        sampleIndex = sampleIndex,
        displayTimeMs = result.frames.getOrNull(sampleIndex)?.displayTimeMs ?: 0L,
        angles = anglesBySample[sampleIndex].orEmpty(),
        faults = result.faults,
    )
}
