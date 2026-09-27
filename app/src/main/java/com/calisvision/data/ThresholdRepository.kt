package com.calisvision.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calisvision.domain.knowledge.ExerciseCatalog
import com.calisvision.domain.rules.AngleThreshold
import com.calisvision.domain.rules.PoseRule
import com.calisvision.domain.rules.RuleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** User-adjusted thresholds for every catalog rule; rules the user never touched carry their knowledge-base default. */
interface ThresholdRepository {
    val thresholds: Flow<Map<RuleId, AngleThreshold>>

    suspend fun set(ruleId: RuleId, threshold: AngleThreshold)

    suspend fun reset()
}

private val catalogRules: List<PoseRule> get() = ExerciseCatalog.all.flatMap { it.rules }

fun defaultThresholds(rules: List<PoseRule> = catalogRules): Map<RuleId, AngleThreshold> = rules.associate { it.id to it.threshold }

private val Context.thresholdStore: DataStore<Preferences> by preferencesDataStore(name = "thresholds")

/**
 * Two floats per rule: Range stores (min, max), Deviation stores (maxExtensionDeg, maxFlexionDeg). The threshold
 * type always follows the rule's default, so a stored pair can never change a rule's kind.
 */
class DataStoreThresholdRepository(context: Context, private val rules: List<PoseRule> = catalogRules) : ThresholdRepository {
    private val store = context.applicationContext.thresholdStore

    override val thresholds: Flow<Map<RuleId, AngleThreshold>> = store.data
        .map { prefs -> rules.associate { rule -> rule.id to read(prefs, rule) } }
        .distinctUntilChanged()

    override suspend fun set(ruleId: RuleId, threshold: AngleThreshold) {
        val (a, b) = when (threshold) {
            is AngleThreshold.Range -> threshold.min to threshold.max
            is AngleThreshold.Deviation -> threshold.maxExtensionDeg to threshold.maxFlexionDeg
        }
        store.edit {
            it[keyA(ruleId)] = a
            it[keyB(ruleId)] = b
        }
    }

    override suspend fun reset() {
        store.edit { it.clear() }
    }

    private fun read(prefs: Preferences, rule: PoseRule): AngleThreshold {
        val a = prefs[keyA(rule.id)] ?: return rule.threshold
        val b = prefs[keyB(rule.id)] ?: return rule.threshold
        return when (rule.threshold) {
            is AngleThreshold.Range -> AngleThreshold.Range(min = a, max = b)
            is AngleThreshold.Deviation -> AngleThreshold.Deviation(maxExtensionDeg = a, maxFlexionDeg = b)
        }
    }

    private fun keyA(id: RuleId) = floatPreferencesKey("${id.value}.a")
    private fun keyB(id: RuleId) = floatPreferencesKey("${id.value}.b")
}

/** For tests and previews: same contract, kept in memory. */
class InMemoryThresholdRepository(rules: List<PoseRule> = catalogRules) : ThresholdRepository {
    private val defaults = defaultThresholds(rules)
    private val state = MutableStateFlow(defaults)

    override val thresholds: Flow<Map<RuleId, AngleThreshold>> = state.asStateFlow()

    override suspend fun set(ruleId: RuleId, threshold: AngleThreshold) {
        state.update { it + (ruleId to threshold) }
    }

    override suspend fun reset() {
        state.value = defaults
    }
}
