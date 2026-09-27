package com.calisvision.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisvision.data.AnalysisSession
import com.calisvision.domain.analysis.FaultEvaluator
import com.calisvision.domain.analysis.FaultSegment
import com.calisvision.domain.model.FramePose
import com.calisvision.domain.model.Joint
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.RuleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.File

/** Image, skeleton and angles of one sample; [faultJoints] are vertices of rules this sample breaks. */
data class FrameOverlay(val pose: FramePose?, val faultJoints: Set<Joint>)

/**
 * [holdFaults] are the listed ones (inside the hold segment), clipped to the hold; [bands] draw every fault in full.
 * [brokenRules] are rules the current sample breaks.
 */
data class ResultUiState(
    val sampleIndex: Int,
    val displayTimeMs: Long,
    val angles: Map<RuleId, Float?>,
    val thresholds: Map<RuleId, AngleThreshold>,
    val brokenRules: Set<RuleId>,
    val faults: List<FaultSegment>,
    val holdFaults: List<FaultSegment>,
    val bands: List<TimelineBand>,
    val selectedFault: FaultSegment? = null,
)

/**
 * The scrubber position is a sampleIndex: frames hold one entry per 0.1 s sample in order (PoseLandmarkerEngine fills
 * index i with sample i), so one step is exactly one sample and image, skeleton and angles share it (AC-7).
 *
 * Faults are re-derived from the stored angle timeline whenever [thresholds] emits; the video is never re-analyzed
 * (AC-9).
 */
class ResultViewModel(
    val session: AnalysisSession,
    thresholds: Flow<Map<RuleId, AngleThreshold>>,
) : ViewModel() {

    val result get() = session.result
    val sampleCount: Int get() = result.frames.size

    private val rules = session.exercise.rules
    private val anglesBySample = result.timeline.frames.associate { it.sampleIndex to it.angles }

    private data class Position(val sampleIndex: Int, val selectedFault: FaultSegment?)
    private data class Evaluation(
        val thresholds: Map<RuleId, AngleThreshold>,
        val faults: List<FaultSegment>,
        val bands: List<TimelineBand>,
    ) {
        val holdFaults: List<FaultSegment> get() = bands.mapNotNull { it.listed }
    }

    private val position = MutableStateFlow(Position(result.holdSegment?.first ?: 0, null))

    private val evaluation: StateFlow<Evaluation> = thresholds
        .map(::evaluate)
        .stateIn(viewModelScope, SharingStarted.Eagerly, evaluate(emptyMap()))

    val state: StateFlow<ResultUiState> = combine(position, evaluation, ::stateOf)
        .stateIn(viewModelScope, SharingStarted.Eagerly, stateOf(position.value, evaluation.value))

    fun seekTo(sampleIndex: Int) {
        if (sampleCount == 0) return
        position.update { it.copy(sampleIndex = sampleIndex.coerceIn(0, sampleCount - 1)) }
    }

    fun step(delta: Int) = seekTo(position.value.sampleIndex + delta)

    /** Opens the fault sheet and moves to the fault's first sample. */
    fun selectFault(fault: FaultSegment) {
        position.value = Position(fault.range.first.coerceIn(0, (sampleCount - 1).coerceAtLeast(0)), fault)
    }

    fun dismissFault() {
        position.update { it.copy(selectedFault = null) }
    }

    fun framePath(sampleIndex: Int): File = File(result.frameDir, "$sampleIndex.jpg")

    fun overlay(sampleIndex: Int): FrameOverlay {
        val broken = brokenRules(sampleIndex, evaluation.value.thresholds)
        val faultJoints = rules.filter { it.id in broken }.mapTo(mutableSetOf()) { it.joints[1] }
        return FrameOverlay(result.frames.getOrNull(sampleIndex), faultJoints)
    }

    /** Stored overrides win; rules without one keep the knowledge-base default. */
    private fun evaluate(overrides: Map<RuleId, AngleThreshold>): Evaluation {
        val effective = rules.associate { it.id to (overrides[it.id] ?: it.threshold) }
        val faults = FaultEvaluator.evaluate(result.timeline, session.exercise, effective)
        val hold = result.holdSegment
        val bands = faults.map { fault ->
            val listed = hold?.takeIf { fault.isInHold(it) }?.let { FaultEvaluator.clip(fault, it, result.timeline, rules, effective) }
            TimelineBand(fault, listed)
        }
        return Evaluation(effective, faults, bands)
    }

    private fun brokenRules(sampleIndex: Int, thresholds: Map<RuleId, AngleThreshold>): Set<RuleId> {
        val angles = anglesBySample[sampleIndex].orEmpty()
        return rules.filter { rule -> rule.isBrokenBy(angles[rule.id], thresholds.getValue(rule.id)) }.mapTo(mutableSetOf()) { it.id }
    }

    private fun stateOf(position: Position, evaluation: Evaluation) = ResultUiState(
        sampleIndex = position.sampleIndex,
        displayTimeMs = result.frames.getOrNull(position.sampleIndex)?.displayTimeMs ?: 0L,
        angles = anglesBySample[position.sampleIndex].orEmpty(),
        thresholds = evaluation.thresholds,
        brokenRules = brokenRules(position.sampleIndex, evaluation.thresholds),
        faults = evaluation.faults,
        holdFaults = evaluation.holdFaults,
        bands = evaluation.bands,
        // A threshold change can remove or reshape the open fault; close the sheet then.
        selectedFault = position.selectedFault?.takeIf { it in evaluation.holdFaults },
    )
}
