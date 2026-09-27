package com.calisvision.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calisvision.data.ThresholdRepository
import com.calisvision.data.defaultThresholds
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.RuleId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: ThresholdRepository) : ViewModel() {

    val thresholds: StateFlow<Map<RuleId, AngleThreshold>> =
        repository.thresholds.stateIn(viewModelScope, SharingStarted.Eagerly, defaultThresholds())

    fun set(ruleId: RuleId, threshold: AngleThreshold) {
        viewModelScope.launch { repository.set(ruleId, threshold) }
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }
}
