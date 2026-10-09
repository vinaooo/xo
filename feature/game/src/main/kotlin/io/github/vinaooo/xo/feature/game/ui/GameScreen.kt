package io.github.vinaooo.xo.feature.game.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.vinkit.achievements.R as BadgesR
import io.github.vinaooo.vinkit.bugreport.ReportTarget
import io.github.vinaooo.vinkit.shell.FrameInfo
import io.github.vinaooo.vinkit.shell.GameFrame
import io.github.vinaooo.vinkit.shell.GameSurface
import io.github.vinaooo.vinkit.shell.GameToolbar
import io.github.vinaooo.vinkit.shell.MenuOption
import io.github.vinaooo.vinkit.shell.NavigationAction
import io.github.vinaooo.vinkit.shell.R as ShellR
import io.github.vinaooo.vinkit.shell.ToolbarAction
import io.github.vinaooo.vinkit.shell.WinCelebration
import io.github.vinaooo.vinkit.shell.WinDialog
import io.github.vinaooo.xo.domain.model.Achievement
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.feature.game.GameIntent
import io.github.vinaooo.xo.feature.game.GameUiState
import io.github.vinaooo.xo.feature.game.GameViewModel
import io.github.vinaooo.xo.feature.game.R
import io.github.vinaooo.xo.feature.game.REPORT_TARGET
import io.github.vinaooo.xo.feature.game.badges.badge
import io.github.vinaooo.xo.feature.game.board.XoBoard
import io.github.vinaooo.xo.feature.game.gameReport
import kotlinx.coroutines.delay

/** The game screen. The Scores and Settings buttons show only when their screens exist (non-null). */
@Composable
fun GameRoute(
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenBadges: (() -> Unit)? = null,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GameScreen(uiState, viewModel::onIntent, modifier, onOpenScores, onOpenSettings, REPORT_TARGET, onOpenBadges)
}

@Composable
fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenScores: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    reportTarget: ReportTarget? = null,
    onOpenBadges: (() -> Unit)? = null,
) {
    val session = uiState.session
    val spoken = uiState.announcement?.let { announcementText(it) }
    GameSurface(
        announcement = spoken,
        announcementSequence = uiState.announcementSequence,
        modifier = modifier,
        reportTarget = reportTarget,
        gameReport = { gameReport(uiState) },
    ) { reportBug ->
        GameFrame(
            settings = uiState.settings,
            info = { frame -> Info(uiState, frame) },
            board = {
                session?.let {
                    val state = it.state
                    val side = state.board.size.side
                    XoBoard(
                        board = state.board,
                        hint = uiState.hint,
                        winningLine = (state.status as? GameStatus.Won)?.line,
                        enabled = uiState.canPlay,
                        onPlace = { cell -> onIntent(GameIntent.Place(cell)) },
                        cellDescription = { cell, mark, hinted -> cellDescription(cell, side, mark, hinted) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            toolbar = { frame -> Toolbar(uiState, onIntent, frame, reportBug) },
            onOpenScores = onOpenScores,
            onOpenSettings = onOpenSettings,
            navigation = listOfNotNull(
                onOpenBadges?.let {
                    NavigationAction(Icons.Rounded.MilitaryTech, stringResource(BadgesR.string.vinkit_badges), it)
                },
            ),
        )
    }
    EndDialog(uiState, onNewGame = { onIntent(GameIntent.NewGame) })
}

/** The mode, and whose turn it is or how the game ended, read as one item. */
@Composable
private fun Info(uiState: GameUiState, frame: FrameInfo) {
    val state = uiState.session?.state ?: return
    val mode = modeName(state.mode)
    val turn = turnText(state, uiState.aiThinking)
    val typography = MaterialTheme.typography
    Column(Modifier.clearAndSetSemantics { contentDescription = "$mode, $turn" }) {
        Text(
            mode,
            style = if (frame.landscape) typography.titleSmall else typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            turn,
            style = if (frame.landscape) typography.headlineMedium else typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Toolbar(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
    frame: FrameInfo,
    onReportBug: (() -> Unit)?,
) {
    val session = uiState.session
    val idle = !uiState.aiThinking
    val hintShown = uiState.hint != null
    GameToolbar(
        actions = listOf(
            ToolbarAction.Button(
                Icons.AutoMirrored.Rounded.Undo,
                stringResource(ShellR.string.vinkit_undo),
                // Also while the AI thinks: undo takes back the move it is answering.
                enabled = session?.canUndo == true,
                keepDirection = true,
            ) { onIntent(GameIntent.Undo) },
            ToolbarAction.Button(
                Icons.AutoMirrored.Rounded.Redo,
                stringResource(ShellR.string.vinkit_redo),
                enabled = idle && session?.canRedo == true,
                keepDirection = true,
            ) { onIntent(GameIntent.Redo) },
            ToolbarAction.Button(
                if (hintShown) Icons.Rounded.Check else Icons.Rounded.Lightbulb,
                stringResource(if (hintShown) R.string.apply_hint else ShellR.string.vinkit_hint),
                enabled = uiState.canPlay,
            ) { onIntent(GameIntent.Hint) },
        ),
        menuOptions = listOf(
            MenuOption(Icons.Rounded.Replay, stringResource(R.string.new_game)) { onIntent(GameIntent.NewGame) },
        ),
        onReportBug = onReportBug,
        vertical = frame.landscape,
        mirrored = frame.mirrored,
    )
}

/**
 * Once a game ends, after the winning line has been struck: the result, the mode and the moves, and a new game. A
 * win the player can be proud of (theirs, or either side's in 2-player) is celebrated.
 */
@Composable
private fun EndDialog(uiState: GameUiState, onNewGame: () -> Unit) {
    val state = uiState.session?.state?.takeIf { it.isOver }
    var shown by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state != null) {
            delay(END_DIALOG_DELAY_MILLIS)
            shown = true
        }
    }
    val celebration = remember(state) { WinCelebration.entries.random() }
    if (state == null || !shown) return
    val status = state.status
    val celebrate = status is GameStatus.Won && (!state.mode.isVsAi || status.mark == GameMode.HUMAN)
    WinDialog(
        lines = listOfNotNull(
            modeName(state.mode),
            stringResource(R.string.moves, state.moves),
            earnedText(uiState.earned),
        ),
        onNewGame = onNewGame,
        title = resultText(status, state.mode.isVsAi),
        kind = celebration.takeIf { celebrate },
    )
}

/** The badges a game just earned: the one by name, or how many. */
@Composable
private fun earnedText(earned: Set<Achievement>): String? = when (earned.size) {
    0 -> null
    1 -> stringResource(R.string.new_badge, badge(earned.first()).name)
    else -> pluralStringResource(R.plurals.new_badges, earned.size, earned.size)
}

/** Long enough to see the winning line struck through before the dialog covers the board. */
private const val END_DIALOG_DELAY_MILLIS = 900L
