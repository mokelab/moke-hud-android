package com.mokelab.hud.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.mokelab.hud.android.internal.HudActivityWatcher
import java.util.concurrent.atomic.AtomicBoolean

/**
 * HUD オーバーレイの公開 API。
 *
 * `install` はホストアプリの `ContentProvider` からのみ呼ばれる想定で internal に留める。
 * 依存に足すだけで動く自動初期化を担保するため、外部から明示的に呼ぶ口は用意しない。
 */
object Hud {
    @Volatile
    private var enabled: Boolean = true

    private val installed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var watcher: HudActivityWatcher? = null

    /** [post] で表示時間を省略したときのデフォルト表示時間（ミリ秒）。 */
    const val DEFAULT_MESSAGE_DURATION_MILLIS: Long = 3_000L

    /** HUD オーバーレイが表示状態かどうか。 */
    val isEnabled: Boolean
        get() = enabled

    /**
     * HUD オーバーレイの表示/非表示を切り替える。任意のスレッドから呼べる。
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        mainHandler.post {
            watcher?.setOverlaysVisible(enabled)
        }
    }

    /**
     * メッセージを HUD オーバーレイに表示する。任意のスレッドから呼べる。
     *
     * 表示中の Activity すべてのオーバーレイに流し込み、[durationMillis] 経過後に
     * 自動で消す。[durationMillis] が 0 以下なら消えず、表示数の上限を超えて古い
     * ものから押し出されるまで残る。
     *
     * [isEnabled] が false のときは何もしない（キューにも溜めない）。非表示中に
     * 積んだメッセージが再表示時に湧いて出るのを避けるため。
     *
     * @param message 表示する文字列。
     * @param durationMillis 表示時間（ミリ秒）。省略時は [DEFAULT_MESSAGE_DURATION_MILLIS]。
     */
    fun post(message: String, durationMillis: Long = DEFAULT_MESSAGE_DURATION_MILLIS) {
        if (!enabled) return
        mainHandler.post {
            watcher?.postMessage(message, durationMillis)
        }
    }

    /**
     * [HudEvent] を HUD オーバーレイに表示する。任意のスレッドから呼べる。
     *
     * [post] の文字列版と違い、[HudEvent.name] を主表示、[HudEvent.params] を詳細行として
     * 描き分ける。表示時間や無効時の挙動は文字列版と同じ（[isEnabled] が false のときは
     * 何もせず、キューにも溜めない）。
     *
     * @param event 表示するイベント。
     * @param durationMillis 表示時間（ミリ秒）。省略時は [DEFAULT_MESSAGE_DURATION_MILLIS]。
     */
    fun post(event: HudEvent, durationMillis: Long = DEFAULT_MESSAGE_DURATION_MILLIS) {
        if (!enabled) return
        // 描画は main スレッドで後から走るため、呼び出しスレッド側で params をスナップショット
        // しておく。呼び出し側が同じマップを post 後に変更（や別スレッドから変更）しても、
        // 呼び出し時点の内容を安定して描画でき、イテレーション中の ConcurrentModificationException
        // も避けられる。表示順を保つため LinkedHashMap でコピーする。空なら不変扱いでそのまま。
        val snapshot = if (event.params.isEmpty()) {
            event
        } else {
            event.copy(params = LinkedHashMap(event.params))
        }
        mainHandler.post {
            watcher?.postEvent(snapshot, durationMillis)
        }
    }

    /**
     * [HudActivityWatcher] を登録し、以降の Activity に自動でオーバーレイを attach する。
     * 二重初期化は [installed] でガードする。
     */
    internal fun install(application: Application) {
        if (!installed.compareAndSet(false, true)) return

        val watcher = HudActivityWatcher()
        this.watcher = watcher
        application.registerActivityLifecycleCallbacks(watcher)
    }
}
