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
 * 描画テキスト履歴で観測する。期限タイマー（[HudOverlayView.showEvent] の durationMillis）は
 * ShadowLooper で時間を進めて発火させる。1 件あたりの描画順は「タイトル → timestamp → 詳細行」
 * なので、[drawnTexts] の並びからそのまま各要素を取り出せる。
 *
 * 注: `View.postDelayed` は未 attach だと RunQueue に溜まって main looper に乗らないため、
 * view は Activity に attach してから検証する（[setUp] で `visible()` まで進める）。
 *
 * 注: `graphicsMode=LEGACY` の `ShadowPaint.measureText` は「文字数」を返し、density は 1.0 に
 * なる。折り返し・省略・高さあふれの検証で幅や高さを既定より小さく取っているのはこのため
 * （既定の [WIDTH] のままでは 1000 文字近く入ってしまい折り返しが起きない）。
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
    fun showEvent_withEmptyParams_drawsTitleAndTimestampOnly() {
        view.showEvent(HudEvent(name = "app_open"), 0L)

        val texts = drawnTexts()
        assertEquals("expected title + timestamp only, was $texts", 2, texts.size)
        assertEquals("app_open", texts[0])
        assertTrue("expected HH:mm:ss, was '${texts[1]}'", TIMESTAMP_PATTERN.matches(texts[1]))
    }

    @Test
    fun showEvent_drawsTimestampForEveryMessage() {
        view.showEvent(HudEvent(name = "plain"), 0L)
        view.showEvent(HudEvent(name = "with_params", params = mapOf("k" to "v")), 0L)

        val timestamps = drawnTexts().filter { TIMESTAMP_PATTERN.matches(it) }
        assertEquals("both bands should carry a timestamp, was $timestamps", 2, timestamps.size)
    }

    @Test
    fun longTitle_isEllipsizedToSingleLine() {
        val title = "a".repeat(120)
        view.showEvent(HudEvent(name = title), 0L)

        val drawnTitle = drawnTexts(width = NARROW_WIDTH)[0]
        assertTrue("expected an ellipsis, was '$drawnTitle'", drawnTitle.endsWith("…"))
        assertTrue("expected it shortened, was ${drawnTitle.length}", drawnTitle.length < title.length)
        assertTrue("expected the head kept, was '$drawnTitle'", drawnTitle.startsWith("aaa"))
    }

    @Test
    fun longDetail_wrapsUpToMaxLinesAndEllipsizesTheLast() {
        val value = List(60) { "w$it" }.joinToString(separator = " ")
        view.showEvent(HudEvent(name = "ev", params = mapOf("k" to value)), 0L)

        // タイトル・timestamp の後ろが詳細行。
        val detailLines = drawnTexts(width = NARROW_WIDTH).drop(2)
        assertEquals("detail should be capped, was $detailLines", 3, detailLines.size)
        assertTrue(
            "the head should be preserved, was '${detailLines.first()}'",
            detailLines.first().startsWith("{k=w0 w1"),
        )
        assertTrue(
            "the last line should be ellipsized, was '${detailLines.last()}'",
            detailLines.last().endsWith("…"),
        )
    }

    @Test
    fun overflowingHeight_dropsOldestBandsAndKeepsNewest() {
        repeat(10) { view.showEvent(HudEvent(name = "ev$it"), 0L) }

        val titles = drawnTexts(height = SHORT_HEIGHT).filter { it.startsWith("ev") }
        assertTrue("expected fewer than 10 bands drawn, was $titles", titles.size < 10)
        assertTrue("newest should stay visible, was $titles", "ev9" in titles)
        assertFalse("oldest should fall out of the draw, was $titles", "ev0" in titles)
    }

    @Test
    fun exceedingMaxMessages_evictsOldestFirst() {
        repeat(12) { view.showEvent(HudEvent(name = "ev$it"), 0L) }

        val titles = drawnTexts().filter { it.startsWith("ev") }
        assertEquals("only MAX_MESSAGES should remain, was $titles", 10, titles.size)
        assertFalse("ev0 should have been evicted", "ev0" in titles)
        assertFalse("ev1 should have been evicted", "ev1" in titles)
        assertTrue("ev2 should remain", "ev2" in titles)
        assertTrue("ev11 should remain", "ev11" in titles)
    }

    @Test
    fun positiveDuration_removesMessageAfterTimeout() {
        view.showEvent(HudEvent(name = "temp"), 3_000L)
        assertTrue("temp" in drawnTexts())

        shadowOf(Looper.getMainLooper()).idleFor(3_000, TimeUnit.MILLISECONDS)

        assertFalse("expected 'temp' removed after timeout", "temp" in drawnTexts())
    }

    @Test
    fun nonPositiveDuration_persistsAfterIdle() {
        view.showEvent(HudEvent(name = "sticky"), 0L)

        shadowOf(Looper.getMainLooper()).idleFor(10, TimeUnit.SECONDS)

        assertTrue("expected 'sticky' to persist without a timer", "sticky" in drawnTexts())
    }

    @Test
    fun detach_clearsMessages() {
        view.showEvent(HudEvent(name = "gone"), 0L)
        assertTrue("gone" in drawnTexts())

        (view.parent as ViewGroup).removeView(view)

        assertTrue("expected no drawn text after detach", drawnTexts().isEmpty())
    }

    /** view をレイアウトして専用 Canvas に描画し、[org.robolectric.shadows.ShadowCanvas] が記録した描画テキストを返す。 */
    private fun drawnTexts(width: Int = WIDTH, height: Int = HEIGHT): List<String> {
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)

        val canvas = Canvas(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888))
        view.draw(canvas)

        val shadow = shadowOf(canvas)
        return (0 until shadow.textHistoryCount).map { shadow.getDrawnTextEvent(it).text }
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1920

        /** 折り返し・省略を起こすための狭い幅（measureText が文字数を返すため小さめに取る）。 */
        const val NARROW_WIDTH = 60

        /** 帯の総高さがあふれる程度に低い高さ。 */
        const val SHORT_HEIGHT = 60

        val TIMESTAMP_PATTERN = Regex("""\d{2}:\d{2}:\d{2}""")
    }
}
