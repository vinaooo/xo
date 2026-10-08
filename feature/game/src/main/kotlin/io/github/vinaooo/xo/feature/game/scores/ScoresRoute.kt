package io.github.vinaooo.xo.feature.game.scores

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.vinkit.scores.ScoresScreen
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.feature.game.ui.opponentName
import io.github.vinaooo.xo.feature.game.ui.sizeName

@Composable
fun ScoresRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: XoScoresViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScoresScreen(
        uiState = uiState,
        onBack = onBack,
        modeName = { key -> GameMode.fromKey(key)?.let { opponentName(it.opponent) } ?: key },
        modifier = modifier,
        groupName = { size -> BoardSize.entries.firstOrNull { it.name == size }?.let { sizeName(it) } ?: size },
        onSelectGroup = viewModel::selectGroup,
        ranked = false,
    )
}
