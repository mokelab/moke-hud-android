package com.mokelab.hud.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HudEventTest {
    @Test
    fun defaults_areEmptyParamsAndCurrentTime() {
        val before = System.currentTimeMillis()
        val event = HudEvent(name = "app_open")
        val after = System.currentTimeMillis()

        assertEquals("app_open", event.name)
        assertEquals(emptyMap<String, Any?>(), event.params)
        assertTrue(event.timestampMillis in before..after)
    }
}
