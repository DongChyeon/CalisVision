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

/** [holdFaults] are the listed ones (inside the hold segment); [brokenRules] are rules the current sample breaks. */
data class ResultUiState(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val angles: Map<RuleId, Float?>,
    val thresholds: Map<RuleId, AngleThreshold>,
    val brokenRules: Set<RuleId>,
    val faults: List<FaultSegment>,
    val holdFaults: List<FaultSegment>,
    val selectedFault: FaultSegment? = null,
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

    private val _state = MutableStateFlow(stateAt(result.holdSegment?.first ?: 0, selectedFault = null))
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    fun seekTo(sampleIndex: Int) {
        if (sampleCount == 0) return
        _state.update { stateAt(sampleIndex.coerceIn(0, sampleCount - 1), it.selectedFault) }
    }

    fun step(delta: Int) = seekTo(state.value.sampleIndex + delta)

    /** Opens the fault sheet and moves to the fault's first sample. */
    fun selectFault(fault: FaultSegment) {
        _state.update { stateAt(fault.range.first.coerceIn(0, (sampleCount - 1).coerceAtLeast(0)), fault) }
    }

    fun dismissFault() {
        _state.update { it.copy(selectedFault = null) }
    }

    fun framePath(sampleIndex: Int): File = File(result.frameDir, "$sampleIndex.jpg")

    fun overlay(sampleIndex: Int): FrameOverlay {
        val broken = brokenRules(sampleIndex)
        val faultJoints = session.exercise.rules.filter { it.id in broken }.mapTo(mutableSetOf()) { it.joints[1] }
        return FrameOverlay(result.frames.getOrNull(sampleIndex), faultJoints)
    }

    private fun brokenRules(sampleIndex: Int): Set<RuleId> {
        val angles = anglesBySample[sampleIndex].orEmpty()
        return session.exercise.rules
            .filter { rule -> rule.isBrokenBy(angles[rule.id], thresholds.getValue(rule.id)) }
            .mapTo(mutableSetOf()) { it.id }
    }

    private fun stateAt(sampleIndex: Int, selectedFault: FaultSegment?) = ResultUiState(
        sampleIndex = sampleIndex,
        displayTimeMs = result.frames.getOrNull(sampleIndex)?.displayTimeMs ?: 0L,
        angles = anglesBySample[sampleIndex].orEmpty(),
        thresholds = thresholds,
        brokenRules = brokenRules(sampleIndex),
        faults = result.faults,
        holdFaults = result.faults.filter { it.isInHold(result.holdSegment) },
        selectedFault = selectedFault,
    )
}
