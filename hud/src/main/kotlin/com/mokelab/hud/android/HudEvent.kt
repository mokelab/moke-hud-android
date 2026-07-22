package com.mokelab.hud.android

/**
 * HUD にオーバーレイ表示する 1 件のイベント。
 */
data class HudEvent(
    val name: String,
    val params: Map<String, Any?> = emptyMap(),
    val timestampMillis: Long = System.currentTimeMillis(),
)
