package com.mokelab.hud.android.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mokelab.hud.android.demo.ui.theme.MokeHudAndroidTheme
import com.mokelab.hud.android.feature.mokera.api.MokeraDetail
import com.mokelab.hud.android.feature.mokera.api.MokeraList
import com.mokelab.hud.android.feature.mokera.impl.mokeraEntries
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MokeHudAndroidTheme {
                MokeraApp()
            }
        }
    }
}

/**
 * mokera の一覧⇄詳細を nav3 の [NavDisplay] で表示するデモのホスト。
 *
 * 画面表示・操作イベントは各画面が :core:analytics:api の AnalyticsLogger 経由で送出し、
 * :core:analytics:debug の束縛によって HUD オーバーレイと logcat に流れる。
 */
@Composable
private fun MokeraApp() {
    val backStack = rememberNavBackStack(MokeraList)
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                mokeraEntries(
                    onMokeraClick = { id -> backStack.add(MokeraDetail(id)) },
                    onBack = { backStack.removeLastOrNull() },
                )
            },
        )
    }
}
