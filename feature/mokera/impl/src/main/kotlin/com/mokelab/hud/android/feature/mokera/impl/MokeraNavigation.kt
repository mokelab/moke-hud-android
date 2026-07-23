package com.mokelab.hud.android.feature.mokera.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.mokelab.hud.android.feature.mokera.api.MokeraDetail
import com.mokelab.hud.android.feature.mokera.api.MokeraList

/**
 * app 側の `NavDisplay` に差し込むための entry provider 拡張。
 *
 * back stack の保持や実際の画面遷移（NavKey の push/pop）は app 側に委ねる。ここでは
 * [MokeraList] / [MokeraDetail] というルートに対する画面の対応付けだけを行う。
 *
 * @param onMokeraClick 一覧で行がタップされたときに呼ばれる。タップされたモケラの id を渡す。
 * @param onBack 詳細画面の TopAppBar の戻る操作で呼ばれる。
 */
fun EntryProviderScope<NavKey>.mokeraEntries(
    onMokeraClick: (String) -> Unit,
    onBack: () -> Unit,
) {
    entry<MokeraList> { MokeraListScreen(onMokeraClick = onMokeraClick) }
    entry<MokeraDetail> { key -> MokeraDetailScreen(id = key.id, onBack = onBack) }
}
