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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * POST されたイベントを画面上部に帯付きで積み上げて表示するオーバーレイ。
 *
 * 各帯はイベント名（[Message.title]）を主表示にし、パラメータ等の詳細（[Message.detail]）が
 * あればその下に一回り小さく描く。[HudEvent.name] をタイトル、[HudEvent.params] を整形して
 * 詳細行に回し、[HudEvent.timestampMillis] を `HH:mm:ss` にしてタイトル行の右端へ置く。
 *
 * 実データの params は長くなりがちなので、タイトルは 1 行に省略（`…`）し、詳細は
 * [MAX_DETAIL_LINES] 行まで折り返して入り切らない分を省略する。積み上げた帯の総高さが
 * View の高さを超える場合は、古い帯から描画対象を外して最新が必ず見えるようにする
 * （[messages] からは消さない。幅・高さが変われば再び収まり得るため）。
 *
 * 各メッセージは [showEvent] で受け取った時間が正なら、その時間経過後に自動で消える。
 * 0 以下を渡した場合はタイマーを張らず、表示数の上限 [MAX_MESSAGES] を超えて押し出されるか
 * View が detach されるまで残り続ける。表示状態の変更・タイマー登録・テキスト計測は
 * すべてメインスレッド上で行われる前提で、[messages] や [timestampFormat] に同期は掛けていない。
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

    /** minSdk 24 では `java.time` が使えないため [SimpleDateFormat] を使う。メインスレッド専用。 */
    private val timestampFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /** 帯内テキストの左右余白。 */
    private val horizontalPadding = density * 12f

    /** 帯内テキストの上下余白。 */
    private val verticalPadding = density * 6f

    /** タイトル行と詳細行、および詳細行同士の行間。 */
    private val lineSpacing = density * 2f

    /** タイトルと右端の timestamp がくっつかないよう最低限空ける間隔。 */
    private val timestampGap = density * 8f

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
     * [HudEvent] を表示に加える。[HudEvent.name] をタイトル、[HudEvent.params] があれば整形して
     * 詳細行に、[HudEvent.timestampMillis] を `HH:mm:ss` にしてタイトル行右端に描く。
     * [durationMillis] が正なら、その時間経過後に自動で消す。メインスレッドから呼ばれる想定。
     */
    fun showEvent(event: HudEvent, durationMillis: Long) {
        val message = Message(
            title = event.name,
            detail = formatParams(event.params),
            timestampText = timestampFormat.format(Date(event.timestampMillis)),
        )
        addMessage(message, durationMillis)
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
        val textRight = right - horizontalPadding
        val contentWidth = textRight - textLeft
        if (contentWidth <= 0f) return

        messages.forEach { layout(it, contentWidth) }

        var top = paddingTop.toFloat()
        for (index in firstVisibleIndex() until messages.size) {
            val message = messages[index]
            val bandHeight = bandHeightOf(message)
            canvas.drawRect(left, top, right, top + bandHeight, bandPaint)

            val contentTop = top + verticalPadding
            // ascent は負値なので、余白の下端からベースラインまで引き下げる。
            val titleBaseline = contentTop - titleFontMetrics.ascent
            canvas.drawText(message.titleLine, textLeft, titleBaseline, titlePaint)
            // timestamp はタイトルと同じベースラインに右詰めで置く。
            canvas.drawText(
                message.timestampText,
                textRight - message.timestampWidth,
                titleBaseline,
                detailPaint,
            )

            var lineTop = contentTop + titleLineHeight
            for (line in message.detailLines) {
                lineTop += lineSpacing
                canvas.drawText(line, textLeft, lineTop - detailFontMetrics.ascent, detailPaint)
                lineTop += detailLineHeight
            }
            top += bandHeight
        }
    }

    /**
     * 描画を開始するインデックスを返す。総高さが View に収まらない場合は古い帯を描画から外し、
     * 最新のものが必ず見えるようにする。最新の 1 件だけは単独で入り切らなくても描く
     * （途中で切れても、何も出ないよりは手掛かりになるため）。
     */
    private fun firstVisibleIndex(): Int {
        val available = (height - paddingTop - paddingBottom).toFloat()
        var used = 0f
        var first = messages.lastIndex
        for (index in messages.indices.reversed()) {
            val bandHeight = bandHeightOf(messages[index])
            if (index < messages.lastIndex && used + bandHeight > available) break
            used += bandHeight
            first = index
        }
        return first
    }

    /**
     * [message] のタイトル行と詳細行を [contentWidth] に合わせて組み直す。[onDraw] は毎フレーム
     * 走るので、計測結果は幅が変わるまで使い回す。
     */
    private fun layout(message: Message, contentWidth: Float) {
        if (message.layoutWidth == contentWidth) return

        message.timestampWidth = detailPaint.measureText(message.timestampText)
        // タイトルは右端の timestamp と重ならない幅に収める。イベント名は通常短いので
        // 折り返さず 1 行に省略する。
        val titleWidth = contentWidth - message.timestampWidth - timestampGap
        message.titleLine = ellipsize(message.title, titlePaint, titleWidth)
        message.detailLines = message.detail
            ?.let { wrap(it, detailPaint, contentWidth, MAX_DETAIL_LINES) }
            ?: emptyList()
        message.layoutWidth = contentWidth
    }

    /** 詳細行の行数に応じて変わる帯の高さを返す。[layout] 済みであることが前提。 */
    private fun bandHeightOf(message: Message): Float {
        val detailHeight = message.detailLines.size * (lineSpacing + detailLineHeight)
        return verticalPadding * 2f + titleLineHeight + detailHeight
    }

    /**
     * [text] を [maxWidth] に収まる 1 行にする。収まらない場合は末尾を [ELLIPSIS] で切る。
     *
     * 幅の判定は [Paint.measureText] だけで行う。`TextUtils.ellipsize` や `StaticLayout` を
     * 使うと Robolectric の LEGACY グラフィックスで計測・描画の記録が期待どおりに動かず、
     * ホスト側テストから描画結果を観測できなくなるため。
     */
    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text

        val ellipsisWidth = paint.measureText(ELLIPSIS)
        // 省略記号すら入らない幅では、はみ出させるより何も描かない。
        if (ellipsisWidth > maxWidth) return ""

        // 省略記号を足しても収まる最長のプレフィックス長を二分探索する。
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (paint.measureText(text.substring(0, mid)) + ellipsisWidth <= maxWidth) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return text.substring(0, low) + ELLIPSIS
    }

    /**
     * [text] を [maxWidth] 幅・最大 [maxLines] 行に折り返す。行数が足りない場合は最終行の末尾を
     * [ELLIPSIS] で切る。params は `, ` 区切りなので、空白優先で折ると語の途中で切れにくい。
     */
    private fun wrap(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        if (maxWidth <= 0f || maxLines <= 0) return emptyList()
        if (paint.measureText(text) <= maxWidth) return listOf(text)

        val lines = mutableListOf<String>()
        var rest = text
        while (rest.isNotEmpty()) {
            if (lines.size == maxLines - 1) {
                // 最終行。残り全部を 1 行に押し込み、入らない分は省略記号で締める。
                lines.add(ellipsize(rest, paint, maxWidth))
                break
            }
            val take = breakAt(rest, paint, maxWidth)
            lines.add(rest.substring(0, take).trimEnd())
            // 折り返し起因の空白が次の行頭に残らないよう読み飛ばす。
            rest = rest.substring(take).trimStart()
        }
        return lines
    }

    /**
     * [text] の先頭から [maxWidth] に収まる文字数を返す。語の途中で切れる場合は直前の空白まで
     * 戻す。空白が無ければ文字単位で切り、必ず 1 文字以上進めて折り返しが止まらないようにする。
     */
    private fun breakAt(text: String, paint: Paint, maxWidth: Float): Int {
        var low = 1
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (paint.measureText(text.substring(0, mid)) <= maxWidth) low = mid else high = mid - 1
        }
        if (low >= text.length) return text.length

        val space = text.lastIndexOf(' ', low - 1)
        return if (space > 0) space + 1 else low
    }

    /**
     * 表示中の 1 件。[title] は主表示（イベント名）、[detail] は任意の詳細行（params の整形結果）、
     * [timestampText] は整形済みの時刻。[remover] は期限タイマーの Runnable で、途中で消すときに
     * 解除する。[titleLine]/[detailLines]/[timestampWidth] は [layout] が [layoutWidth] の幅に
     * 対して組んだ結果のキャッシュ。
     */
    private class Message(
        val title: String,
        val detail: String?,
        val timestampText: String,
    ) {
        var remover: Runnable? = null
        var titleLine: String = title
        var detailLines: List<String> = emptyList()
        var timestampWidth: Float = 0f

        /** [titleLine]/[detailLines] を組んだときの幅。負値は未計算を表す。 */
        var layoutWidth: Float = -1f
    }

    private companion object {
        /** 同時に表示するメッセージ数の上限。超えた分は古いものから捨てる。 */
        const val MAX_MESSAGES = 10

        /** 1 件あたりの詳細行の最大行数。超える分は最終行を省略記号で締める。 */
        const val MAX_DETAIL_LINES = 3

        const val ELLIPSIS = "…"
    }
}
