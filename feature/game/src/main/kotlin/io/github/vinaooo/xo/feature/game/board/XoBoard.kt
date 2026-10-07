package io.github.vinaooo.xo.feature.game.board

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.Mark

/**
 * The board: grid lines, X and O drawn as strokes that animate in, the [hint] cell lit up, and the [winningLine]
 * struck through once the game is won. One invisible node per cell, row by row, tells TalkBack what's there and
 * takes its double tap.
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
            hint?.let { drawHint(geometry, it, colors.tertiaryContainer) }
            drawGrid(side, colors.outlineVariant)
            board.cells.forEachIndexed { cell, mark ->
                when (mark) {
                    Mark.X -> drawX(geometry, cell, progress[cell].value, colors.primary)
                    Mark.O -> drawO(geometry, cell, progress[cell].value, colors.tertiary)
                    null -> Unit
                }
            }
            winningLine?.let { line ->
                val mark = board[line.first()] ?: return@let
                drawStrike(geometry, line, strike.value, if (mark == Mark.X) colors.primary else colors.tertiary)
            }
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

private fun DrawScope.drawGrid(side: Int, color: Color) {
    val cell = size.width / side
    val stroke = GRID_STROKE.toPx()
    for (i in 1 until side) {
        val at = cell * i
        drawLine(color, Offset(at, 0f), Offset(at, size.height), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, at), Offset(size.width, at), stroke, StrokeCap.Round)
    }
}

private fun DrawScope.drawHint(geometry: BoardGeometry, cell: Int, color: Color) {
    val (x, y) = geometry.center(cell)
    val half = geometry.cellPx / 2 - HINT_INSET.toPx()
    drawRoundRect(color, Offset(x - half, y - half), Size(half * 2, half * 2), CornerRadius(HINT_CORNER.toPx()))
}

/** X: the first stroke over the first half of [progress], the second over the rest. */
private fun DrawScope.drawX(geometry: BoardGeometry, cell: Int, progress: Float, color: Color) {
    val (x, y) = geometry.center(cell)
    val r = geometry.cellPx * MARK_RADIUS
    val stroke = geometry.cellPx * MARK_STROKE
    val first = (progress * 2).coerceIn(0f, 1f)
    val second = (progress * 2 - 1).coerceIn(0f, 1f)
    drawLine(color, Offset(x - r, y - r), Offset(x - r + 2 * r * first, y - r + 2 * r * first), stroke, StrokeCap.Round)
    if (second > 0f) {
        drawLine(
            color,
            Offset(x + r, y - r),
            Offset(x + r - 2 * r * second, y - r + 2 * r * second),
            stroke,
            StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawO(geometry: BoardGeometry, cell: Int, progress: Float, color: Color) {
    val (x, y) = geometry.center(cell)
    val r = geometry.cellPx * MARK_RADIUS
    drawArc(
        color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(x - r, y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(geometry.cellPx * MARK_STROKE, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawStrike(geometry: BoardGeometry, line: List<Int>, progress: Float, color: Color) {
    val (x0, y0) = geometry.center(line.first())
    val (x1, y1) = geometry.center(line.last())
    val end = Offset(x0 + (x1 - x0) * progress, y0 + (y1 - y0) * progress)
    drawLine(color.copy(alpha = STRIKE_ALPHA), Offset(x0, y0), end, geometry.cellPx * STRIKE_STROKE, StrokeCap.Round)
}

internal const val BOARD_TAG = "board"
private const val MARK_MILLIS = 250
private const val STRIKE_MILLIS = 400
private const val MARK_RADIUS = 0.28f
private const val MARK_STROKE = 0.09f
private const val STRIKE_STROKE = 0.12f
private const val STRIKE_ALPHA = 0.85f
private val GRID_STROKE = 3.dp
private val HINT_INSET = 6.dp
private val HINT_CORNER = 12.dp
