package com.calisvision.domain.knowledge

import com.calisvision.domain.rules.Exercise

object ExerciseCatalog {
    val all: List<Exercise> = listOf(HandstandKnowledge.exercise)

    fun byId(id: String): Exercise? = all.firstOrNull { it.id == id }
}
