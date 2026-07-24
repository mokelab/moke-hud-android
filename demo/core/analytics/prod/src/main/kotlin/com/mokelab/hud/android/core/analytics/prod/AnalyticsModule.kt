package com.mokelab.hud.android.core.analytics.prod

import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger
import com.mokelab.hud.android.core.analytics.impl.AnalyticsLoggerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 本番系の Analytics 配線。[AnalyticsLoggerImpl] を [AnalyticsLogger] として提供する。
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideAnalyticsLogger(): AnalyticsLogger = AnalyticsLoggerImpl()
}
