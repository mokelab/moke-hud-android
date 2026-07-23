package com.mokelab.hud.android.internal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mokelab.hud.android.HudEvent

/**
 * POST されたイベント/メッセージを画面上部に帯付きで積み上げて表示するオーバーレイ。
 *
 * 各帯はイベント名（[Message.title]）を主表示にし、パラメータ等の詳細（[Message.detail]）が
 * あればその下に一回り小さく描く。[HudEvent] で渡された場合は [HudEvent.name] をタイトル、
 * [HudEvent.params] を整形して詳細行に回す。プレーン文字列で渡された場合はタイトルのみ。
 *
 * 各メッセージは [showMessage]/[showEvent] で受け取った時間が正なら、その時間経過後に
 * 自動で消える。0 以下を渡した場合はタイマーを張らず、表示数の上限 [MAX_MESSAGES] を
 * 超えて押し出されるか View が detach されるまで残り続ける。表示状態の変更・
 * タイマー登録はすべてメインスレッド上で行われる前提で、[messages] へのアクセスに
 * 同期は掛けていない。
 */
internal class HudOverlayView(context: Context) : View(context) {
    private val density = context.resources.displayMetrics.density

    private val bandPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
    }
    private val titlePaint = Paint().apply {
        color = Color.WHITE
        textSize = density * 16f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }
    private val detailPaint = Paint().apply {
        color = Color.rgb(200, 200, 200)
        textSize = density * 13f
        isAntiAlias = true
    }

    /** 帯内テキストの左右余白。 */
    private val horizontalPadding = density * 12f

    /** 帯内テキストの上下余白。 */
    private val verticalPadding = density * 6f

    /** タイトル行と詳細行の行間。 */
    private val lineSpacing = density * 2f

    private val titleFontMetrics = titlePaint.fontMetrics
    private val detailFontMetrics = detailPaint.fontMetrics
    private val titleLineHeight = titleFontMetrics.descent - titleFontMetrics.ascent
    private val detailLineHeight = detailFontMetrics.descent - detailFontMetrics.ascent

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
     * プレーンなメッセージを表示に加える（詳細行なし）。[durationMillis] が正なら、その時間
     * 経過後に自動で消す。メインスレッドから呼ばれる想定。
     */
    fun showMessage(message: String, durationMillis: Long) {
        addMessage(Message(title = message, detail = null), durationMillis)
    }

    /**
     * [HudEvent] を表示に加える。[HudEvent.name] をタイトル、[HudEvent.params] があれば整形して
     * 詳細行に描く。[durationMillis] が正なら、その時間経過後に自動で消す。メインスレッドから
     * 呼ばれる想定。
     */
    fun showEvent(event: HudEvent, durationMillis: Long) {
        addMessage(Message(title = event.name, detail = formatParams(event.params)), durationMillis)
    }

    /** 1 件をリストに積み、上限超過分を捨て、期限タイマーを張って再描画する共通処理。 */
    private fun addMessage(entry: Message, durationMillis: Long) {
        messages.add(entry)
        // 大量に POST されても描画が破綻しないよう、古いものから捨てる。
        // 再描画は末尾の invalidate() 一回にまとめるため、ここでは discard に留める。
        while (messages.size > MAX_MESSAGES) {
            discard(messages.first())
        }
        if (durationMillis > 0) {
            val remover = Runnable { removeMessage(entry) }
            entry.remover = remover
            postDelayed(remover, durationMillis)
        }
        invalidate()
    }

    /** [HudEvent.params] を `{k=v, ...}` に整形する。空なら詳細行を出さない（null を返す）。 */
    private fun formatParams(params: Map<String, Any?>): String? {
        if (params.isEmpty()) return null
        return params.entries.joinToString(separator = ", ", prefix = "{", postfix = "}") {
            "${it.key}=${it.value}"
        }
    }

    /** 期限タイマーから呼ばれ、1 件消して再描画する。 */
    private fun removeMessage(entry: Message) {
        if (discard(entry)) invalidate()
    }

    /** リストから外してタイマーを解除するだけの共通処理。再描画は呼び出し側に任せる。 */
    private fun discard(entry: Message): Boolean {
        if (!messages.remove(entry)) return false
        entry.remover?.let(::removeCallbacks)
        return true
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
        val textLeft = left + horizontalPadding
        var top = paddingTop.toFloat()
        for (message in messages) {
            val bandHeight = bandHeightOf(message)
            canvas.drawRect(left, top, right, top + bandHeight, bandPaint)

            val contentTop = top + verticalPadding
            // ascent は負値なので、余白の下端からベースラインまで引き下げる。
            canvas.drawText(message.title, textLeft, contentTop - titleFontMetrics.ascent, titlePaint)
            message.detail?.let { detail ->
                val detailBaseline = contentTop + titleLineHeight + lineSpacing - detailFontMetrics.ascent
                canvas.drawText(detail, textLeft, detailBaseline, detailPaint)
            }
            top += bandHeight
        }
    }

    /** 詳細行の有無で高さが変わる帯の高さを返す。 */
    private fun bandHeightOf(message: Message): Float {
        val contentHeight = if (message.detail != null) {
            titleLineHeight + lineSpacing + detailLineHeight
        } else {
            titleLineHeight
        }
        return verticalPadding * 2f + contentHeight
    }

    /**
     * 表示中の 1 件。[title] は主表示（イベント名/メッセージ）、[detail] は任意の詳細行
     * （params の整形結果）。[remover] は期限タイマーの Runnable で、途中で消すときに解除する。
     */
    private class Message(val title: String, val detail: String?) {
        var remover: Runnable? = null
    }

    private companion object {
        /** 同時に表示するメッセージ数の上限。超えた分は古いものから捨てる。 */
        const val MAX_MESSAGES = 10
    }
}
