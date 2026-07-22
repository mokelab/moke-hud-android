package com.mokelab.hud.android.demo

import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `:hud` の internal クラスに触れずに、
 * ContentProvider 自動初期化 → decorView への attach が成立していることを確認する。
 */
@RunWith(AndroidJUnit4::class)
class HudOverlayAttachTest {
    @Test
    fun hudOverlay_isAttachedToDecorView() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val decorView = activity.window.decorView as ViewGroup
                val hasOverlay = decorView.children().any { it.contentDescription == "MokeHud overlay" }
                assertTrue(hasOverlay)
            }
        }
    }

    private fun ViewGroup.children(): List<android.view.View> =
        (0 until childCount).map { getChildAt(it) }
}
