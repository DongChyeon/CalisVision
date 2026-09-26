package com.calisvision.fixtures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseFixtureTest {

    @Test
    fun parsesSchema() {
        val landmark = """{"x":0.5,"y":0.9,"z":-0.1,"visibility":0.98}"""
        val json = """{"width":1080,"height":1920,"frames":[
            {"sampleIndex":0,"displayTimeMs":0,"landmarks":[${List(33) { landmark }.joinToString(",")}]},
            {"sampleIndex":1,"displayTimeMs":100,"landmarks":null}]}"""
        val fixture = PoseFixture.parse(json)
        assertEquals(1080f / 1920f, fixture.aspect, 1e-6f)
        assertEquals(33, fixture.frames[0].landmarks!!.size)
        assertEquals(0.98f, fixture.frames[0][15]!!.visibility, 1e-6f)
        assertNull(fixture.frames[1].landmarks)
        assertEquals(100L, fixture.frames[1].displayTimeMs)
    }
}
