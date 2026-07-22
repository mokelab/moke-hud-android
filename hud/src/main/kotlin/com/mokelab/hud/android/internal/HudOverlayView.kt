package com.mokelab.hud.android.internal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * POST されたメッセージを画面上部に帯付きで積み上げて表示するオーバーレイ。
 *
 * 各メッセージは [showMessage] で受け取った時間だけ表示され、期限が来ると自動で
 * 消える。表示状態の変更・タイマー登録はすべてメインスレッド上で行われる前提で、
 * [messages] へのアクセスに同期は掛けていない。
 */
internal class HudOverlayView(context: Context) : View(context) {
    private val bandPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = context.resources.displayMetrics.density * 16f
        isAntiAlias = true
    }
    private val bandHeight = context.resources.displayMetrics.density * 32f

    /** 表示中のメッセージ。新しいものほど後ろに積まれ、上から順に描画される。 */
    private val messages = mutableListOf<Message>()

    init {
        // ホストアプリの操作を妨げないよう、タッチは一切消費せずすり抜けさせる。
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
        // contentDescription はテストからの識別用に残しつつ、ホストアプリの
        // TalkBack 探索に混ざらないようアクセシビリティツリーからは除外する。
        contentDescription = "MokeHud overlay"
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO

        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            // insets は消費せず、下の View 階層にもそのまま伝播させる。
            insets
        }
    }

    /**
     * メッセージを表示に加える。[durationMillis] が正なら、その時間経過後に自動で消す。
     * メインスレッドから呼ばれる想定。
     */
    fun showMessage(message: String, durationMillis: Long) {
        val entry = Message(message)
        messages.add(entry)
        // 大量に POST されても描画が破綻しないよう、古いものから捨てる。
        while (messages.size > MAX_MESSAGES) {
            removeMessage(messages.first())
        }
        if (durationMillis > 0) {
            val remover = Runnable { removeMessage(entry) }
            entry.remover = remover
            postDelayed(remover, durationMillis)
        }
        invalidate()
    }

    private fun removeMessage(entry: Message) {
        if (!messages.remove(entry)) return
        entry.remover?.let(::removeCallbacks)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // detach 後に期限タイマーが残ると View への参照が残り続けるので確実に外す。
        messages.forEach { it.remover?.let(::removeCallbacks) }
        messages.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (messages.isEmpty()) return

        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        var top = paddingTop.toFloat()
        for (message in messages) {
            canvas.drawRect(left, top, right, top + bandHeight, bandPaint)
            canvas.drawText(
                message.text,
                left + bandHeight / 2f,
                top + bandHeight * 0.7f,
                textPaint,
            )
            top += bandHeight
        }
    }

    /** 表示中の 1 件。[remover] は期限タイマーの Runnable で、途中で消すときに解除する。 */
    private class Message(val text: String) {
        var remover: Runnable? = null
    }

    private companion object {
        /** 同時に表示するメッセージ数の上限。超えた分は古いものから捨てる。 */
        const val MAX_MESSAGES = 10
    }
}
