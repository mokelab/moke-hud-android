package com.mokelab.hud.android.core.analytics.debug

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [HudAnalyticsLogger] が delegate へ呼び出しを素通しすることを検証する。
 *
 * `Hud.post` を経由するため（`Hud` object 初期化で main Looper に触れる）Robolectric 上で回す。
 * HUD 側の見た目（name/params の描き分け）は `:hud` の HudOverlayViewTest がカバーするので、
 * ここでは「実処理を担う delegate に元の引数がそのまま渡ること」に絞る。
 */
@RunWith(AndroidJUnit4::class)
class HudAnalyticsLoggerTest {
    private val delegate = FakeAnalyticsLogger()
    private val logger = HudAnalyticsLogger(delegate)

    @Test
    fun screenView_forwardsScreenNameAndClassToDelegate() {
        logger.screenView(screenName = "mokera_list", screenClass = "MokeraListScreen")

        assertEquals(listOf("mokera_list" to "MokeraListScreen"), delegate.screenViews)
    }

    @Test
    fun screenView_forwardsNullScreenClass() {
        logger.screenView(screenName = "mokera_list", screenClass = null)

        assertEquals(listOf("mokera_list" to null), delegate.screenViews)
    }

    @Test
    fun logEvent_forwardsNameAndParamsUnchanged() {
        val params = mapOf("mokera_id" to "1", "mokera_name" to "緑茶モケラ")

        logger.logEvent(name = "like_mokera", params = params)

        assertEquals(listOf("like_mokera" to params), delegate.events)
    }

    @Test
    fun calls_doNotThrowWhenHudNotInstalled() {
        // watcher 未登録（HUD 未 install）でも Hud.post 経路が例外を投げないこと。
        logger.screenView(screenName = "s", screenClass = null)
        logger.logEvent(name = "e", params = emptyMap())

        assertEquals(1, delegate.screenViews.size)
        assertEquals(1, delegate.events.size)
    }

    /** 受け取った呼び出しをそのまま記録する delegate 用のフェイク。 */
    private class FakeAnalyticsLogger : AnalyticsLogger {
        val screenViews = mutableListOf<Pair<String, String?>>()
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()

        override fun screenView(screenName: String, screenClass: String?) {
            screenViews += screenName to screenClass
        }

        override fun logEvent(name: String, params: Map<String, Any?>) {
            events += name to params
        }
    }
}
