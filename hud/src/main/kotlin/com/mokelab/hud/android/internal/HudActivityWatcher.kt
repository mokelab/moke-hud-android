package com.mokelab.hud.android.internal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.mokelab.hud.android.Hud
import java.util.WeakHashMap

/**
 * 全 Activity の `window.decorView` にオーバーレイを attach/detach するコールバック。
 * Activity をリークさせないよう、保持は [WeakHashMap] で行う。
 */
internal class HudActivityWatcher : Application.ActivityLifecycleCallbacks {
    private val overlays = WeakHashMap<Activity, HudOverlayView>()

    override fun onActivityStarted(activity: Activity) {
        // 冪等ガード: 再 start では二重に attach しない。
        if (overlays.containsKey(activity)) return

        val decorView = activity.window?.decorView as? ViewGroup ?: return
        val overlay = HudOverlayView(activity)
        overlay.visibility = if (Hud.isEnabled) View.VISIBLE else View.GONE
        decorView.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
