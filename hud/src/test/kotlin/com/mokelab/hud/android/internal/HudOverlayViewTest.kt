package com.mokelab.hud.android.internal

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mokelab.hud.android.HudEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import java.util.concurrent.TimeUnit

/**
 * [HudOverlayView] の表示ロジックを Robolectric で検証する。
 *
 * 本体は無改変のまま、`view.draw(canvas)` の結果を [org.robolectric.shadows.ShadowCanvas] の
 * 描画テキスト履歴で観測する。期限タイマー（[HudOverlayView.showMessage] の durationMillis）は
 * ShadowLooper で時間を進めて発火させる。
 *
 * 注: `View.postDelayed` は未 attach だと RunQueue に溜まって main looper に乗らないため、
 * view は Activity に attach してから検証する（[setUp] で `visible()` まで進める）。
 */
@RunWith(AndroidJUnit4::class)
class HudOverlayViewTest {
    private lateinit var controller: ActivityController<Activity>
    private lateinit var view: HudOverlayView

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(Activity::class.java).create()
        val activity = controller.get()
        view = HudOverlayView(activity)
        // visible() より前に content view に入れておくことで、attach 時に mAttachInfo が付き
        // postDelayed が main handler に直接乗るようになる。
        activity.setContentView(view)
        controller.start().resume().visible()
    }

    @After
    fun tearDown() {
        controller.pause().stop().destroy()
    }

    @Test
    fun showEvent_drawsNameAndFormattedParams() {
        view.showEvent(HudEvent(name = "like_mokera", params = mapOf("mokera_id" to "1")), 0L)

        val texts = drawnTexts()
        assertTrue("expected title 'like_mokera' in $texts", "like_mokera" in texts)
        assertTrue("expected detail '{mokera_id=1}' in $texts", "{mokera_id=1}" in texts)
    }

    @Test
    fun showEvent_withEmptyParams_drawsTitleOnly() {
        view.showEvent(HudEvent(name = "app_open"), 0L)

        assertEquals(listOf("app_open"), drawnTexts())
    }

    @Test
    fun showMessage_drawsPlainTitleOnly() {
        view.showMessage("hello", 0L)

        assertEquals(listOf("hello"), drawnTexts())
    }

    @Test
    fun exceedingMaxMessages_evictsOldestFirst() {
        repeat(12) { view.showMessage("ev$it", 0L) }

        val texts = drawnTexts()
        assertEquals("only MAX_MESSAGES should remain, was $texts", 10, texts.size)
        assertFalse("ev0 should have been evicted", "ev0" in texts)
        assertFalse("ev1 should have been evicted", "ev1" in texts)
        assertTrue("ev2 should remain", "ev2" in texts)
        assertTrue("ev11 should remain", "ev11" in texts)
    }

    @Test
    fun positiveDuration_removesMessageAfterTimeout() {
        view.showMessage("temp", 3_000L)
        assertTrue("temp" in drawnTexts())

        shadowOf(Looper.getMainLooper()).idleFor(3_000, TimeUnit.MILLISECONDS)

        assertFalse("expected 'temp' removed after timeout", "temp" in drawnTexts())
    }

    @Test
    fun nonPositiveDuration_persistsAfterIdle() {
        view.showMessage("sticky", 0L)

        shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)

        assertTrue("expected 'sticky' to persist without a timer", "sticky" in drawnTexts())
    }

    @Test
    fun detach_clearsMessages() {
        view.showMessage("gone", 0L)
        assertTrue("gone" in drawnTexts())

        (view.parent as ViewGroup).removeView(view)

        assertTrue("expected no drawn text after detach", drawnTexts().isEmpty())
    }

    /** view をレイアウトして専用 Canvas に描画し、[org.robolectric.shadows.ShadowCanvas] が記録した描画テキストを返す。 */
    private fun drawnTexts(): List<String> {
        view.measure(
            MeasureSpec.makeMeasureSpec(WIDTH, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(HEIGHT, MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, WIDTH, HEIGHT)

        val canvas = Canvas(Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888))
        view.draw(canvas)

        val shadow = shadowOf(canvas)
        return (0 until shadow.textHistoryCount).map { shadow.getDrawnTextEvent(it).text }
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1920
    }
}
