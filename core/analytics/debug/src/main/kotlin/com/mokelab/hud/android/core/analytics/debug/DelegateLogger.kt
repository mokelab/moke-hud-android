package com.mokelab.hud.android.core.analytics.debug

import javax.inject.Qualifier

/** HUD 装飾の内側で実処理を担う delegate 用の AnalyticsLogger を指す qualifier。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DelegateLogger
