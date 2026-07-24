package com.mokelab.hud.android.core.analytics.impl

import android.util.Log
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger

/**
 * AnalyticsLogger のプレースホルダ実装。現状は logcat に出力するのみ。
 */
class AnalyticsLoggerImpl : AnalyticsLogger {
    override fun screenView(screenName: String, screenClass: String?) {
        Log.d("logger", "screenView: screenName=$screenName, screenClass=$screenClass")
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        Log.d("logger", "logEvent: name=$name, params=$params")
    }
}
