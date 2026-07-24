package com.mokelab.hud.android.core.analytics.debug

import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger
import com.mokelab.hud.android.core.analytics.impl.AnalyticsLoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * デバッグ系の Analytics 配線。実処理を [AnalyticsLoggerImpl] に委ねつつ、
 * 公開する [AnalyticsLogger] は HUD へ流す [HudAnalyticsLogger] にする。
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    @DelegateLogger
    fun provideDelegate(): AnalyticsLogger = AnalyticsLoggerImpl()

    @Provides
    @Singleton
    fun provideAnalyticsLogger(
        @DelegateLogger delegate: AnalyticsLogger,
    ): AnalyticsLogger = HudAnalyticsLogger(delegate)
}
