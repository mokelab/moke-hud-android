package com.mokelab.hud.android

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.mokelab.hud.android.internal.HudActivityWatcher
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Public API for the HUD overlay.
 *
 * `install` is deliberately kept internal: it is meant to be called only from the library's
 * own `ContentProvider`. No explicit entry point is exposed to callers, so that adding the
 * dependency is all it takes for the HUD to initialize itself.
 */
object Hud {
    @Volatile
    private var enabled: Boolean = true

    private val installed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var watcher: HudActivityWatcher? = null

    /** Default display duration in milliseconds, used when [post] omits it. */
    const val DEFAULT_MESSAGE_DURATION_MILLIS: Long = 3_000L

    /** Whether the HUD overlay is currently shown. */
    val isEnabled: Boolean
        get() = enabled

    /**
     * Shows or hides the HUD overlay. Safe to call from any thread.
     */
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        mainHandler.post {
            watcher?.setOverlaysVisible(enabled)
        }
    }

    /**
     * Shows a message on the HUD overlay. Safe to call from any thread.
     *
     * The message is fed to the overlay of every resumed Activity and dismissed
     * automatically once [durationMillis] has elapsed. If [durationMillis] is 0 or less the
     * message never expires, and stays until it is pushed out oldest-first once the message
     * count exceeds its cap.
     *
     * Does nothing while [isEnabled] is false — messages are dropped rather than queued, so
     * that ones posted while hidden do not suddenly pop up when the overlay is shown again.
     *
     * @param message the text to display.
     * @param durationMillis how long to display it, in milliseconds. Defaults to
     *   [DEFAULT_MESSAGE_DURATION_MILLIS].
     */
    fun post(message: String, durationMillis: Long = DEFAULT_MESSAGE_DURATION_MILLIS) {
        if (!enabled) return
        mainHandler.post {
            watcher?.postMessage(message, durationMillis)
        }
    }

    /**
     * Shows a [HudEvent] on the HUD overlay. Safe to call from any thread.
     *
     * Unlike the [String] overload of [post], [HudEvent.name] is drawn as the main title and
     * [HudEvent.params] as a separate detail line. Display duration and the behavior while
     * disabled match the [String] overload (does nothing while [isEnabled] is false, and
     * nothing is queued).
     *
     * @param event the event to display.
     * @param durationMillis how long to display it, in milliseconds. Defaults to
     *   [DEFAULT_MESSAGE_DURATION_MILLIS].
     */
    fun post(event: HudEvent, durationMillis: Long = DEFAULT_MESSAGE_DURATION_MILLIS) {
        if (!enabled) return
        // Drawing happens later on the main thread, so snapshot params here on the calling
        // thread. Even if the caller mutates the same map after post (or from another thread),
        // the content as of the call is drawn reliably, and ConcurrentModificationException
        // during iteration is avoided. Copy into a LinkedHashMap to preserve display order.
        // An empty map needs no copy — treat it as immutable and pass it through.
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
     * Registers a [HudActivityWatcher] so that overlays are attached automatically to every
     * subsequent Activity. Double initialization is guarded by [installed].
     */
    internal fun install(application: Application) {
        if (!installed.compareAndSet(false, true)) return

        val watcher = HudActivityWatcher()
        this.watcher = watcher
        application.registerActivityLifecycleCallbacks(watcher)
    }
}
