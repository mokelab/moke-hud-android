package com.mokelab.hud.android.feature.mokera.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mokelab.hud.android.core.analytics.api.AnalyticsLogger

/**
 * モケラ詳細画面。
 *
 * @param id 表示対象のモケラ id（[com.mokelab.hud.android.feature.mokera.api.MokeraDetail] の id）。
 * @param onBack TopAppBar の戻る操作で呼ばれる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MokeraDetailScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MokeraDetailViewModel = hiltViewModel<MokeraDetailViewModel, MokeraDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) },
    ),
    analytics: AnalyticsLogger = rememberAnalyticsLogger(),
) {
    LaunchedEffect(id) {
        analytics.screenView(screenName = "mokera_detail", screenClass = "MokeraDetailScreen")
    }
    val mokeraName = viewModel.mokera?.let { stringResource(it.nameRes) }
    MokeraDetailContent(
        mokera = viewModel.mokera,
        onBack = onBack,
        onLike = {
            viewModel.mokera?.let { mokera ->
                analytics.logEvent(
                    name = "like_mokera",
                    params = mapOf("mokera_id" to mokera.id, "mokera_name" to mokeraName.orEmpty()),
                )
            }
        },
        modifier = modifier,
    )
}

/** ViewModel に依存しない stateless な詳細表示。Preview から利用する。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MokeraDetailContent(
    mokera: Mokera?,
    onBack: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(mokera?.let { stringResource(it.nameRes) }.orEmpty()) },
                navigationIcon = {
                    // material-icons-core を依存に追加しないため、同梱の vector drawable を使う。
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (mokera != null) {
                Text(text = stringResource(mokera.nameRes), style = MaterialTheme.typography.headlineSmall)
                Text(text = stringResource(mokera.descriptionRes), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onLike, modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = stringResource(R.string.mokera_like))
                }
            } else {
                Text(text = stringResource(R.string.mokera_not_found))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MokeraDetailScreenPreview() {
    MokeraDetailContent(mokera = sampleMokeraList.first(), onBack = {}, onLike = {})
}
