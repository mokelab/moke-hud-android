package com.mokelab.hud.android.feature.mokera.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
        items(mokeraList, key = { it.id }) { mokera ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMokeraClick(mokera.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = mokera.name, style = MaterialTheme.typography.titleMedium)
                Text(text = mokera.description, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MokeraListScreenPreview() {
    MokeraListContent(mokeraList = sampleMokeraList, onMokeraClick = {})
}
