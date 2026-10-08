package io.github.vinaooo.xo.feature.game.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.Mark

/**
 * The board, drawn by hand ([drawHandDrawnBoard]): a pen grid, X and O that draw themselves in with a bounce, the
 * [hint] cell lit up, and the [winningLine] struck through once the game is won. One invisible node per cell, row by
 * row, tells TalkBack what's there and takes its double tap.
 */
@Composable
internal fun XoBoard(
    board: Board,
    hint: Int?,
    winningLine: List<Int>?,
    enabled: Boolean,
    onPlace: (Int) -> Unit,
    cellDescription: @Composable (cell: Int, mark: Mark?, hinted: Boolean) -> String,
    modifier: Modifier = Modifier,
) {
    val side = board.size.side
    val colors = MaterialTheme.colorScheme
    val progress = board.cells.indices.map { cell -> markProgress(cell, board[cell]) }
    val pop = board.cells.indices.map { cell -> markPop(cell, board[cell]) }
    val strike = remember(winningLine) { Animatable(0f) }
    LaunchedEffect(winningLine) { if (winningLine != null) strike.animateTo(1f, tween(STRIKE_MILLIS)) }
    Box(modifier.testTag(BOARD_TAG)) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(enabled, side) {
                if (enabled) {
                    detectTapGestures { tap ->
                        BoardGeometry(side, size.width.toFloat()).cellAt(tap.x, tap.y)?.let(onPlace)
                    }
                }
            },
        ) {
            val geometry = BoardGeometry(side, size.width)
            drawHandDrawnBoard(
                BoardPicture(board, hint, winningLine),
                geometry,
                BoardMotion({ progress[it].value }, { pop[it].value }, strike.value),
                colors,
            )
        }
        CellNodes(board, hint, enabled, onPlace, cellDescription)
    }
}

/** How far a cell's mark is drawn, 0 to 1; a new mark draws itself in. */
@Composable
private fun markProgress(cell: Int, mark: Mark?): Animatable<Float, *> {
    val progress = remember(cell, mark) { Animatable(0f) }
    LaunchedEffect(cell, mark) { if (mark != null) progress.animateTo(1f, tween(MARK_MILLIS)) }
    return progress
}

/** A new mark's size: it pops in from [POP_FROM] and springs past full size before settling (the bounce). */
@Composable
private fun markPop(cell: Int, mark: Mark?): Animatable<Float, *> {
    val pop = remember(cell, mark) { Animatable(POP_FROM) }
    LaunchedEffect(cell, mark) {
        if (mark != null) pop.animateTo(1f, spring(dampingRatio = POP_DAMPING, stiffness = Spring.StiffnessMediumLow))
    }
    return pop
}

@Composable
private fun CellNodes(
    board: Board,
    hint: Int?,
    enabled: Boolean,
    onPlace: (Int) -> Unit,
    cellDescription: @Composable (cell: Int, mark: Mark?, hinted: Boolean) -> String,
) {
    val side = board.size.side
    Column(Modifier.fillMaxSize()) {
        repeat(side) { row ->
            Row(Modifier.weight(1f)) {
                repeat(side) { column ->
                    val cell = row * side + column
                    val description = cellDescription(cell, board[cell], cell == hint)
                    Box(
                        Modifier.weight(1f).fillMaxSize().semantics {
                            contentDescription = description
                            selected = cell == hint
                            if (enabled && board[cell] == null) {
                                onClick {
                                    onPlace(cell)
                                    true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

internal const val BOARD_TAG = "board"
private const val POP_FROM = 0.7f
private const val POP_DAMPING = 0.45f
private const val MARK_MILLIS = 250
private const val STRIKE_MILLIS = 400
