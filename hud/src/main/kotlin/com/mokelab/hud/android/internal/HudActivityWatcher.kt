package com.mokelab.hud.android.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.mokelab.hud.android.Hud

/**
 * 全 Activity の `window.decorView` にオーバーレイを attach/detach するコールバック。
 *
 * オーバーレイは Context として Activity を強参照するため、弱参照のマップに入れても
 * value から key への参照が残ってエントリは回収されない。リークを防いでいるのは
 * [onActivityDestroyed] での確実な remove であり、保持自体は通常のマップで足りる。
 */
internal class HudActivityWatcher : Application.ActivityLifecycleCallbacks {
    private val overlays = mutableMapOf<Activity, HudOverlayView>()

    override fun onActivityStarted(activity: Activity) {
        // 冪等ガード: 再 start では二重に attach しない。
        if (overlays.containsKey(activity)) return

        val decorView = activity.window?.decorView as? ViewGroup ?: return
        val overlay = HudOverlayView(activity)
        overlay.visibility = if (Hud.isEnabled) View.VISIBLE else View.GONE
        // decorView は ViewGroup としてしか扱わないので、親の型を仮定しない
        // LayoutParams を渡す。親が独自の型を要求する場合は addView 側で変換される。
        decorView.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        overlays[activity] = overlay
    }

    override fun onActivityDestroyed(activity: Activity) {
        val overlay = overlays.remove(activity) ?: return
        (overlay.parent as? ViewGroup)?.removeView(overlay)
    }

    /** [Hud.setEnabled] からメインスレッド経由で呼ばれ、attach 済み全オーバーレイの表示を切り替える。 */
    internal fun setOverlaysVisible(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        overlays.values.forEach { it.visibility = visibility }
    }

    /** [Hud.post] からメインスレッド経由で呼ばれ、attach 済み全オーバーレイにメッセージを流す。 */
    internal fun postMessage(message: String, durationMillis: Long) {
        overlays.values.forEach { it.showMessage(message, durationMillis) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
