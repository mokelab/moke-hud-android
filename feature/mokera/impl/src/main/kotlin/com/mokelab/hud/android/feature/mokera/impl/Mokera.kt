package com.mokelab.hud.android.feature.mokera.impl

/**
 * デモ用のモケラ（架空のマスコット）1体を表すモデル。
 *
 * モケラはクリエイターの近くにある飲み物から誕生する生き物。どの飲み物から生まれたかで
 * 見た目や性格が変わる。List-Detail デモに閉じたデータで、他モジュールには公開しない。
 */
data class Mokera(
    val id: String,
    val name: String,
    val description: String,
)

/** デモ用にハードコードしたモケラの一覧。それぞれ身近な飲み物から生まれている。 */
internal val sampleMokeraList: List<Mokera> = listOf(
    Mokera(
        id = "1",
        name = "緑茶モケラ",
        description = "湯呑みの緑茶から生まれたモケラ。落ち着いた性格で、ほっと一息つきたいときにそばに現れる。",
    ),
    Mokera(
        id = "2",
        name = "コーヒーモケラ",
        description = "淹れたてのコーヒーから生まれたモケラ。夜更かし作業のお供で、いつも眠そうにしているのに元気。",
    ),
    Mokera(
        id = "3",
        name = "ミルクモケラ",
        description = "コップのミルクから生まれたモケラ。まっしろでふわふわ。人懐っこく、誰にでもすぐなつく。",
    ),
    Mokera(
        id = "4",
        name = "オレンジジュースモケラ",
        description = "オレンジジュースから生まれたモケラ。いつも元気いっぱいで、朝いちばんに姿を現すことが多い。",
    ),
    Mokera(
        id = "5",
        name = "メロンソーダモケラ",
        description = "メロンソーダから生まれたモケラ。しゅわしゅわとよく跳ねる。気分屋で、機嫌がそのまま泡になる。",
    ),
)
