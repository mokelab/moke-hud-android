package com.mokelab.hud.android.feature.mokera.impl

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * モケラ詳細画面の状態を保持する ViewModel。
 *
 * nav3 の entry は route（[com.mokelab.hud.android.feature.mokera.api.MokeraDetail]）から
 * 取り出した `id` を [MokeraDetailScreen] へそのまま渡す（[MokeraNavigation] 参照）。
 * この ViewModel は Hilt の assisted injection でその `id` を受け取り、該当データを解決する。
 */
@HiltViewModel(assistedFactory = MokeraDetailViewModel.Factory::class)
class MokeraDetailViewModel @AssistedInject constructor(
    @Assisted private val id: String,
) : ViewModel() {

    val mokera: Mokera? = sampleMokeraList.firstOrNull { it.id == id }

    @AssistedFactory
    interface Factory {
        fun create(id: String): MokeraDetailViewModel
    }
}
