package com.mokelab.hud.android.feature.mokera.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * ViewModel を介さず Composable から Hilt 提供の [AnalyticsLogger]（Singleton）を取り出すための
 * EntryPoint。束縛は app 側（:core:analytics:prod か :core:analytics:debug）が担う。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AnalyticsLoggerEntryPoint {
    fun analyticsLogger(): AnalyticsLogger
}

/**
 * Composable から [AnalyticsLogger] を取得する。application context 単位で解決し remember する。
 */
@Composable
internal fun rememberAnalyticsLogger(): AnalyticsLogger {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, AnalyticsLoggerEntryPoint::class.java)
            .analyticsLogger()
    }
}
