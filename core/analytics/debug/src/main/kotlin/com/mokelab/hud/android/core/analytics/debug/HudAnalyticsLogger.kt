package com.mokelab.hud.android.core.analytics.debug

import com.mokelab.hud.android.Hud
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger

/**
 * Analytics 呼び出しを HUD に可視化しつつ、実処理を [delegate] へ委譲するデバッグ用 Logger。
 */
class HudAnalyticsLogger(
    private val delegate: AnalyticsLogger,
) : AnalyticsLogger {
    override fun screenView(screenName: String, screenClass: String?) {
        Hud.post("screen: $screenName")
        delegate.screenView(screenName, screenClass)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        val message = if (params.isEmpty()) {
            name
        } else {
            val body = params.entries.joinToString(prefix = "{", postfix = "}") {
                "${it.key}=${it.value}"
            }
            "$name $body"
        }
        Hud.post(message)
        delegate.logEvent(name, params)
    }
}
