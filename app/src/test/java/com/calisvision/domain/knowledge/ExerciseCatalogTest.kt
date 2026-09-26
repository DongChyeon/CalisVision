package com.calisvision.domain.knowledge

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun looksUpHandstandById() {
        assertSame(HandstandKnowledge.exercise, ExerciseCatalog.byId("handstand"))
        assertNull(ExerciseCatalog.byId("unknown"))
    }
}
