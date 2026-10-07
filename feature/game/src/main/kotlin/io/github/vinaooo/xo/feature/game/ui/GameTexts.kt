package io.github.vinaooo.xo.feature.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.feature.game.Announcement
import io.github.vinaooo.xo.feature.game.R

/** "4×4 · Hard", "3×3 · 2 players". */
@Composable
fun modeName(mode: GameMode): String =
    stringResource(R.string.mode_name, sizeName(mode.size), opponentName(mode.opponent))

@Composable
fun sizeName(size: BoardSize): String = stringResource(R.string.board_size, size.side, size.side)

@Composable
fun opponentName(opponent: Opponent): String = stringResource(
    when (opponent) {
        Opponent.EASY -> R.string.opponent_easy
        Opponent.MEDIUM -> R.string.opponent_medium
        Opponent.HARD -> R.string.opponent_hard
        Opponent.TWO_PLAYER -> R.string.opponent_two_player
    },
)

/** Whose turn it is, or how the game ended. */
@Composable
internal fun turnText(state: GameState, aiThinking: Boolean): String = when (val status = state.status) {
    GameStatus.Playing -> when {
        !state.mode.isVsAi -> stringResource(R.string.turn_of, markName(state.toMove))
        aiThinking || state.isAiTurn -> stringResource(R.string.ai_thinking)
        else -> stringResource(R.string.your_turn)
    }
    else -> resultText(status, state.mode.isVsAi)
}

/** "You won!", "You lost", "Draw", "X wins!". */
@Composable
internal fun resultText(status: GameStatus, vsAi: Boolean): String = when {
    status is GameStatus.Won && !vsAi -> stringResource(R.string.mark_wins, markName(status.mark))
    status is GameStatus.Won && status.mark == GameMode.HUMAN -> stringResource(R.string.you_won)
    status is GameStatus.Won -> stringResource(R.string.you_lost)
    else -> stringResource(R.string.draw)
}

@Composable
internal fun markName(mark: Mark): String = stringResource(if (mark == Mark.X) R.string.mark_x else R.string.mark_o)

/** "row 2, column 3", counted from 1. */
@Composable
internal fun cellName(cell: Int, side: Int): String = stringResource(
    R.string.a11y_cell,
    cell / side + 1,
    cell % side + 1,
)

/** A cell for TalkBack: "row 2, column 3, X", "…, empty", "…, empty, suggested". */
@Composable
internal fun cellDescription(cell: Int, side: Int, mark: Mark?, hinted: Boolean): String {
    val content = mark?.let { markName(it) } ?: stringResource(R.string.a11y_empty)
    val described = stringResource(R.string.a11y_cell_content, cellName(cell, side), content)
    return if (hinted) stringResource(R.string.a11y_cell_hinted, described) else described
}

@Composable
internal fun announcementText(announcement: Announcement): String = when (announcement) {
    is Announcement.Placed -> stringResource(
        R.string.a11y_placed,
        markName(announcement.mark),
        cellName(announcement.cell, announcement.side),
    )
    is Announcement.Hinted -> stringResource(R.string.a11y_hinted, cellName(announcement.cell, announcement.side))
    Announcement.Undone -> stringResource(io.github.vinaooo.vinkit.shell.R.string.vinkit_undone)
    Announcement.Redone -> stringResource(io.github.vinaooo.vinkit.shell.R.string.vinkit_redone)
    is Announcement.Ended -> resultText(announcement.status, announcement.vsAi)
}
