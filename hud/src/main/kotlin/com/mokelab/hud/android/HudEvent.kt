package com.mokelab.hud.android

/**
 * A single event to be shown on the HUD overlay.
 */
data class HudEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
    val timestampMillis: Long = System.currentTimeMillis(),
)
