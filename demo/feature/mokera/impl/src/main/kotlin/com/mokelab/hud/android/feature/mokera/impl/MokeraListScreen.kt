package com.mokelab.hud.android.feature.mokera.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger

/**
 * モケラ一覧画面。
 *
 * @param onMokeraClick 行タップ時に呼ばれる。タップされたモケラの id を渡す。
 */
@Composable
fun MokeraListScreen(
    onMokeraClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MokeraListViewModel = hiltViewModel(),
    analytics: AnalyticsLogger = rememberAnalyticsLogger(),
) {
    LaunchedEffect(Unit) {
        analytics.screenView(screenName = "mokera_list", screenClass = "MokeraListScreen")
    }
    val mokeraList by viewModel.uiState.collectAsState()
    MokeraListContent(
        mokeraList = mokeraList,
        onMokeraClick = onMokeraClick,
        modifier = modifier,
    )
}

/** ViewModel に依存しない stateless な一覧表示。Preview から利用する。 */
@Composable
private fun MokeraListContent(
    mokeraList: List<Mokera>,
    onMokeraClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            MokeraListHeader()
            HorizontalDivider()
        }
        items(mokeraList, key = { it.id }) { mokera ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMokeraClick(mokera.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = stringResource(mokera.nameRes), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(mokera.descriptionRes), style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

/**
 * 一覧の先頭に置く、このデモと HUD の説明バンド。
 *
 * このアプリが何のデモかと、HUD を実際に映すための有効化手順を伝える。HUD の既定表示は
 * 手動差し替え運用のため、「デバッグビルドで自動表示」ではなく差し替え手順として案内する
 * （文言は demo/app/build.gradle.kts の該当コメントと整合させている）。
 */
@Composable
private fun MokeraListHeader(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.mokera_demo_header_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.mokera_demo_header_description),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.mokera_demo_header_enable_hud),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MokeraListScreenPreview() {
    MokeraListContent(mokeraList = sampleMokeraList, onMokeraClick = {})
}
