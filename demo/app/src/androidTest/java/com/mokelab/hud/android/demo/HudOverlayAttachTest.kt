package com.mokelab.hud.android.demo

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `:hud` の internal クラスに触れずに、ContentProvider 自動初期化 →
 * オーバーレイ用サブウィンドウの attach が成立していることを確認する。
 *
 * オーバーレイは decorView の子ではなく WindowManager の独立ウィンドウとして足されるため、
 * その window（root view = オーバーレイ自身）を root matcher で特定して検証する。
 */
@RunWith(AndroidJUnit4::class)
class HudOverlayAttachTest {
    @Test
    fun hudOverlay_isAttachedAsSeparateWindow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withContentDescription(OVERLAY_DESCRIPTION))
                .inRoot(isHudOverlayWindow())
                .check(matches(isDisplayed()))
        }
    }

    /** root view（= 追加されたオーバーレイ）の contentDescription で HUD の window を選ぶ。 */
    private fun isHudOverlayWindow(): TypeSafeMatcher<Root> = object : TypeSafeMatcher<Root>() {
        override fun describeTo(description: Description) {
            description.appendText("HUD overlay window with root contentDescription '$OVERLAY_DESCRIPTION'")
        }

        override fun matchesSafely(root: Root): Boolean {
            val decorView: View = root.decorView
            return decorView.contentDescription == OVERLAY_DESCRIPTION
        }
    }

    private companion object {
        const val OVERLAY_DESCRIPTION = "MokeHud overlay"
    }
}
