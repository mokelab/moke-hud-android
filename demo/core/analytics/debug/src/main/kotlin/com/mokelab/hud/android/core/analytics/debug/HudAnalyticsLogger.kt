package com.mokelab.hud.android.core.analytics.debug

import com.mokelab.hud.android.Hud
import com.mokelab.hud.android.HudEvent
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger

/**
 * Analytics 呼び出しを HUD に可視化しつつ、実処理を [delegate] へ委譲するデバッグ用 Logger。
 *
 * 呼び出しは [HudEvent] として構造のまま HUD に渡す（name/params の描き分けは HUD 側が担う）。
 */
class HudAnalyticsLogger(
    private val delegate: AnalyticsLogger,
) : AnalyticsLogger {
    override fun screenView(screenName: String, screenClass: String?) {
        val params = buildMap {
            put("screen_name", screenName)
            if (screenClass != null) put("screen_class", screenClass)
        }
        Hud.post(HudEvent(name = "screen_view", params = params))
        delegate.screenView(screenName, screenClass)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        Hud.post(HudEvent(name = name, params = params))
        delegate.logEvent(name, params)
    }
}
