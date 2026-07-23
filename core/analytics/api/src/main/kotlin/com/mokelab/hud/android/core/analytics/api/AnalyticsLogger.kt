package com.mokelab.hud.android.core.analytics.api

/**
 * アプリの Analytics イベントを通知する抽象。
 *
 * 実装は Firebase Analytics 等の SDK に委譲する想定（:core:analytics:prod）で、
 * デバッグ時は同じ呼び出しを HUD に流し込む実装（:core:analytics:debug）へ差し替える。
 */
interface AnalyticsLogger {
    /**
     * 画面表示を通知する。Firebase の screen_view 相当。
     *
     * @param screenName 画面名（screen_name）。
     * @param screenClass 画面のクラス名（screen_class）。省略時は実装側の既定に委ねる。
     */
    fun screenView(screenName: String, screenClass: String? = null)

    /**
     * 任意のイベントを通知する。
     *
     * @param name イベント名。
     * @param params イベントに付随するパラメータ。Firebase の Bundle 相当。
     */
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}
