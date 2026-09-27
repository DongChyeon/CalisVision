package com.calisvision.data

import com.calisvision.domain.model.AnalysisResult
import com.calisvision.domain.rules.Exercise
import java.util.concurrent.ConcurrentHashMap

data class AnalysisSession(val result: AnalysisResult, val exercise: Exercise) {
    val id: String get() = result.sessionId
}

/**
 * Holds finished analyses in memory only (user decision 2026-09-27): results are gone when the process dies,
 * and a result screen restored without its session returns home. Frame JPEGs are cleaned at process start.
 */
class AnalysisSessionStore {
    private val sessions = ConcurrentHashMap<String, AnalysisSession>()

    fun put(session: AnalysisSession) {
        sessions[session.id] = session
    }

    operator fun get(sessionId: String): AnalysisSession? = sessions[sessionId]
}
