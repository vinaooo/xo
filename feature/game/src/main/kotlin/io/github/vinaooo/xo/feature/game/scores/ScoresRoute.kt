package io.github.vinaooo.xo.feature.game.scores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.vinkit.scores.ScoresScreen
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.feature.game.ui.modeName

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: XoScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(
        uiState = uiState,
        onBack = onBack,
        modeName = { key -> GameMode.fromKey(key)?.let { modeName(it) } ?: key },
        modifier = modifier,
        onSelectMode = viewModel::selectMode,
        ranked = false,
    )
}
