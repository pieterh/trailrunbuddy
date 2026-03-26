package com.trailrunbuddy.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatHhMmSs formats zero as 00-00-00`() {
        assertEquals("00:00:00", TimeFormatter.formatHhMmSs(0))
    }

    @Test
    fun `formatHhMmSs formats seconds only`() {
        assertEquals("00:00:45", TimeFormatter.formatHhMmSs(45))
    }

    @Test
    fun `formatHhMmSs formats minutes and seconds`() {
        assertEquals("00:05:30", TimeFormatter.formatHhMmSs(330))
    }

    @Test
    fun `formatHhMmSs formats hours minutes and seconds`() {
        assertEquals("02:15:07", TimeFormatter.formatHhMmSs(8107))
    }

    @Test
    fun `formatHhMmSsFromMs converts milliseconds correctly`() {
        assertEquals("00:01:00", TimeFormatter.formatHhMmSsFromMs(60_000))
    }

    @Test
    fun `formatHhMmSsFromMs clamps negative values to zero`() {
        assertEquals("00:00:00", TimeFormatter.formatHhMmSsFromMs(-5000))
    }

    @Test
    fun `formatMmSs formats minutes and seconds`() {
        assertEquals("05:30", TimeFormatter.formatMmSs(330))
    }

    @Test
    fun `formatMmSs handles exactly one hour`() {
        assertEquals("60:00", TimeFormatter.formatMmSs(3600))
    }
}
