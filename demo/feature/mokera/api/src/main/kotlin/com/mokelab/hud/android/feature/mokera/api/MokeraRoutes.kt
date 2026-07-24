package com.mokelab.hud.android.feature.mokera.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** モケラ一覧画面のルート。 */
@Serializable
data object MokeraList : NavKey

/** モケラ詳細画面のルート。表示対象を [id] で指定する。 */
@Serializable
data class MokeraDetail(val id: String) : NavKey
