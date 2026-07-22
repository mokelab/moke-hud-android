package com.mokelab.hud.android.internal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * attach が効いていることを目視確認するためのプレースホルダ表示。
 * イベント描画は未実装で、画面上部に半透明の帯とラベルを出すだけ。
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            paddingTop + bandHeight,
            bandPaint,
        )
        canvas.drawText(
            "MokeHud",
            paddingLeft + bandHeight / 2f,
            paddingTop + bandHeight * 0.7f,
            textPaint,
        )
    }
}
