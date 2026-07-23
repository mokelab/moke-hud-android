package com.mokelab.hud.android.internal

import android.app.Activity
import android.app.Application
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.mokelab.hud.android.Hud

/**
 * 全 Activity に、その window へ紐づくオーバーレイ用のサブウィンドウを attach/detach する
 * コールバック。
 *
 * オーバーレイは decorView の子ではなく [WindowManager] の独立ウィンドウ
 * （[WindowManager.LayoutParams.TYPE_APPLICATION_PANEL]）として足す。自 Activity の window
 * トークンに紐づくサブウィンドウなので `SYSTEM_ALERT_WINDOW` は不要で、かつ独立した
 * ViewRootImpl / Surface を持つため、ホスト（Compose 等）の描画に相乗りせずとも
 * [HudOverlayView.invalidate] だけで確実に自前フレームが再合成される（decorView の子に
 * 後付けすると、静止状態での再描画が画面に届かず表示・消去が反映されない問題があった）。
 *
 * オーバーレイは Context として Activity を強参照するため、弱参照のマップに入れても
 * value から key への参照が残ってエントリは回収されない。リークを防いでいるのは
 * [onActivityDestroyed] での確実な remove であり、保持自体は通常のマップで足りる。
 */
internal class HudActivityWatcher : Application.ActivityLifecycleCallbacks {
    private val overlays = mutableMapOf<Activity, HudOverlayView>()

    override fun onActivityResumed(activity: Activity) {
        // 冪等ガード: resume が繰り返されても二重に attach しない。
        if (overlays.containsKey(activity)) return

        // onResume の dispatch 時点では decorView がまだ WindowManager に追加されておらず
        // （追加は直後の handleResumeActivity 内）、サブウィンドウに必要な windowToken が
        // 無効。次のループに回すと decorView は attach 済みでトークンが有効になる。
        val decorView = activity.window?.decorView ?: return
        decorView.post { attachOverlay(activity) }
    }

    /** windowToken が有効になった後（[onActivityResumed] からの post 経由）にオーバーレイを attach する。 */
    private fun attachOverlay(activity: Activity) {
        // post が二重に積まれても、また既に destroy されていても、ここで弾く。
        if (overlays.containsKey(activity)) return
        val windowToken = activity.window?.decorView?.windowToken ?: return
        val overlay = HudOverlayView(activity)
        overlay.visibility = if (Hud.isEnabled) View.VISIBLE else View.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            // フォーカスもタッチも一切奪わず、ホストアプリの操作をそのまま素通しする。
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                // status bar 領域まで含めて全画面に敷きつつ、insets を受け取れるようにする
                // （HudOverlayView 側が system bars 分の padding を付ける）。
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
            PixelFormat.TRANSLUCENT,
        ).apply {
            token = windowToken
            gravity = Gravity.TOP or Gravity.START
        }

        // window が既に破棄されるなどで addView が失敗しても、ホストアプリを巻き込まない。
        val added = runCatching { activity.windowManager.addView(overlay, params) }
            .onFailure { Log.w("HudActivityWatcher", "overlay addView failed", it) }
            .isSuccess
        if (added) overlays[activity] = overlay
    }

    override fun onActivityDestroyed(activity: Activity) {
        val overlay = overlays.remove(activity) ?: return
        // 既に window が外れていると removeView は例外を投げるため保護する。
        runCatching { activity.windowManager.removeView(overlay) }
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

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
