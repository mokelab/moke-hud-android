package com.mokelab.hud.android.feature.mokera.impl

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** モケラ一覧画面の状態を保持する ViewModel。 */
@HiltViewModel
class MokeraListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(sampleMokeraList)
    val uiState: StateFlow<List<Mokera>> = _uiState.asStateFlow()
}
