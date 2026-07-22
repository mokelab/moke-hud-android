package com.mokelab.hud.android.internal

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import com.mokelab.hud.android.Hud

/**
 * `AndroidManifest.xml` に宣言され、アプリ起動時に自動生成される `ContentProvider`。
 * `onCreate()` の時点で `Application` インスタンスは生成済みのため、
 * ホストアプリの `Application` クラスを一切改変せずに [Hud.install] を呼べる。
 */
internal class HudInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        // false は provider の初期化失敗を意味する。Application が取れないのは
        // テスト用 Context などの例外的なケースなので、HUD を諦めるだけに留めて
        // ホストアプリの起動には影響させない。
        val application = context?.applicationContext as? Application
        if (application != null) {
            Hud.install(application)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): android.database.Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
