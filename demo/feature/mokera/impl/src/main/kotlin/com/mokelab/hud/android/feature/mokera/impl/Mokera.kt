package com.mokelab.hud.android.feature.mokera.impl

import androidx.annotation.StringRes

/**
 * デモ用のモケラ（架空のマスコット）1体を表すモデル。
 *
 * モケラはクリエイターの近くにある飲み物から誕生する生き物。どの飲み物から生まれたかで
 * 見た目や性格が変わる。List-Detail デモに閉じたデータで、他モジュールには公開しない。
 * 名前・説明はロケールごとに切り替えられるよう文字列リソースの id で持つ。
 */
data class Mokera(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
)

/** デモ用にハードコードしたモケラの一覧。それぞれ身近な飲み物から生まれている。 */
internal val sampleMokeraList: List<Mokera> = listOf(
    Mokera(
        id = "1",
        nameRes = R.string.mokera_name_green_tea,
        descriptionRes = R.string.mokera_description_green_tea,
    ),
    Mokera(
        id = "2",
        nameRes = R.string.mokera_name_coffee,
        descriptionRes = R.string.mokera_description_coffee,
    ),
    Mokera(
        id = "3",
        nameRes = R.string.mokera_name_milk,
        descriptionRes = R.string.mokera_description_milk,
    ),
    Mokera(
        id = "4",
        nameRes = R.string.mokera_name_orange_juice,
        descriptionRes = R.string.mokera_description_orange_juice,
    ),
    Mokera(
        id = "5",
        nameRes = R.string.mokera_name_melon_soda,
        descriptionRes = R.string.mokera_description_melon_soda,
    ),
)
