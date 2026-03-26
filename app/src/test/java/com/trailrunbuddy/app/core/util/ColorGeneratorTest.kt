package com.trailrunbuddy.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorGeneratorTest {

    @Test
    fun `fromName returns a hex color string`() {
        val color = ColorGenerator.fromName("Marathon")
        assertTrue(color.startsWith("#"))
        assertEquals(7, color.length)
    }

    @Test
    fun `fromName is deterministic for the same input`() {
        val a = ColorGenerator.fromName("Trail")
        val b = ColorGenerator.fromName("Trail")
        assertEquals(a, b)
    }

    @Test
    fun `fromName returns different colors for different names`() {
        // Not guaranteed for all inputs, but palette has 10 entries so highly likely
        val results = (1..10).map { ColorGenerator.fromName("Profile$it") }.toSet()
        assertTrue(results.size > 1)
    }

    @Test
    fun `fromName handles empty string without crash`() {
        assertNotNull(ColorGenerator.fromName(""))
    }
}
