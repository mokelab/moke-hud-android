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
