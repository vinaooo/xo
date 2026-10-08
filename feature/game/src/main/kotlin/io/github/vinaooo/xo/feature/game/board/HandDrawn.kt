package io.github.vinaooo.xo.feature.game.board

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.Mark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** What the board shows: the marks, the [hint] cell, and the [winningLine] once there is one. */
internal class BoardPicture(val board: Board, val hint: Int?, val winningLine: List<Int>?)

/** Where each animation is: a mark's strokes drawn in ([progress]), its size ([pop], the bounce), the [strike]. */
internal class BoardMotion(val progress: (Int) -> Float, val pop: (Int) -> Float, val strike: Float)

/**
 * The board drawn by hand (user's choice): a wavering pen grid, X and O in four shapes each, picked by their cell so a
 * board looks varied and a mark never changes shape while it's on the board, and the winning line struck with the
 * same pen.
 */
internal fun DrawScope.drawHandDrawnBoard(
    picture: BoardPicture,
    geometry: BoardGeometry,
    motion: BoardMotion,
    colors: ColorScheme,
) {
    val board = picture.board
    picture.hint?.let { drawHintCell(geometry, it, colors.tertiaryContainer) }
    drawSketchGrid(board.size.side, colors.onSurfaceVariant)
    board.cells.forEachIndexed { cell, mark ->
        if (mark != null) {
            val (x, y) = geometry.center(cell)
            val pop = motion.pop(cell)
            val pen = Pen(markColor(mark, colors), geometry.cellPx * MARK_STROKE * pop)
            drawSketchMark(
                Spot(Offset(x, y), geometry.cellPx * MARK_RADIUS * pop),
                cell,
                mark,
                motion.progress(cell),
                pen,
            )
        }
    }
    picture.winningLine?.let { line ->
        drawStrike(geometry, line, motion.strike, markColor(board[line.first()], colors))
    }
}

/** A mark's [center] and its radius [r]. */
private class Spot(val center: Offset, val r: Float)

private class Pen(val color: Color, val width: Float) {
    val style = Stroke(width, cap = StrokeCap.Round)
}

private fun markColor(mark: Mark?, colors: ColorScheme) = if (mark == Mark.X) colors.primary else colors.tertiary

private fun DrawScope.drawHintCell(geometry: BoardGeometry, cell: Int, color: Color) {
    val (x, y) = geometry.center(cell)
    val half = geometry.cellPx / 2 * HINT_SCALE
    drawRoundRect(
        color,
        Offset(x - half, y - half),
        Size(half * 2, half * 2),
        CornerRadius(
            geometry.cellPx * HINT_CORNER,
        ),
    )
}

/** Grid lines that waver like a pen; the same wobble every frame (a fixed phase per line). */
private fun DrawScope.drawSketchGrid(side: Int, color: Color) {
    val cell = size.width / side
    val style = Stroke(size.width * GRID_STROKE, cap = StrokeCap.Round)
    val amplitude = size.width * GRID_WOBBLE
    for (i in 1 until side) {
        val at = cell * i
        drawPath(
            traced { t ->
                waver(Offset(at, 0f), Offset(at, size.height), t, amplitude, i * GRID_PHASE_V)
            },
            color,
            style = style,
        )
        drawPath(
            traced { t ->
                waver(Offset(0f, at), Offset(size.width, at), t, amplitude, i * GRID_PHASE_H)
            },
            color,
            style = style,
        )
    }
}

/** The point [t] of the way from [from] to [to], pushed sideways by a gentle wave. */
private fun waver(from: Offset, to: Offset, t: Float, amplitude: Float, phase: Float): Offset {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = hypot(dx, dy)
    val side = sin(t * PI.toFloat() * GRID_WAVES + phase) * amplitude
    return Offset(from.x + dx * t - dy / length * side, from.y + dy * t + dx / length * side)
}

/** A mark in one of its four shapes, [progress] of the way drawn: an X's first stroke then its second; an O's loop. */
private fun DrawScope.drawSketchMark(spot: Spot, cell: Int, mark: Mark, progress: Float, pen: Pen) {
    val center = spot.center
    val r = spot.r
    val variant = (cell * VARIANT_MIX + mark.ordinal) % X_SHAPES.size
    if (mark == Mark.X) {
        val shape = X_SHAPES[variant]
        val first = (progress * 2).coerceIn(0f, 1f)
        val second = (progress * 2 - 1).coerceIn(0f, 1f)
        val down = shape.stroke(center, r, -1f, shape.first.reach, 0f)
        drawPath(stroke(down, shape.first.bow * r, cell, first), pen.color, style = pen.style)
        if (second > 0f) {
            val up = shape.stroke(center, r, 1f, shape.second.reach, shape.shift)
            drawPath(stroke(up, shape.second.bow * r, cell + 1, second, shape.flick), pen.color, style = pen.style)
        }
    } else {
        drawPath(loop(center, r, cell, O_SHAPES[variant], progress), pen.color, style = pen.style)
    }
}

private fun DrawScope.drawStrike(geometry: BoardGeometry, line: List<Int>, progress: Float, color: Color) {
    // The same pen as the marks: a slightly bowed stroke, past the end cells' centers.
    val (x0, y0) = geometry.center(line.first())
    val (x1, y1) = geometry.center(line.last())
    val from = Offset(x0 - (x1 - x0) * STRIKE_OVERSHOOT, y0 - (y1 - y0) * STRIKE_OVERSHOOT)
    val to = Offset(x1 + (x1 - x0) * STRIKE_OVERSHOOT, y1 + (y1 - y0) * STRIKE_OVERSHOOT)
    drawPath(
        stroke(from to to, geometry.cellPx * STRIKE_BOW, line.first(), progress),
        color.copy(alpha = STRIKE_ALPHA),
        style = Stroke(geometry.cellPx * STRIKE_STROKE, cap = StrokeCap.Round),
    )
}

/** One stroke of an X: how far it [reach]es past the mark's radius and how much it [bow]s sideways. */
private class XStroke(val reach: Float, val bow: Float = 0f)

/**
 * One of the four quick pen X's: [tiltDegrees], its [first] and [second] strokes, the second's [shift] off center, its
 * [proportions] (wide to tall), and a small [flick] at the second stroke's end.
 */
private class XShape(
    val tiltDegrees: Float,
    val first: XStroke,
    val second: XStroke,
    val shift: Float = 0f,
    val proportions: Pair<Float, Float> = 1f to 1f,
    val flick: Boolean = false,
) {
    /** A stroke from the top corner on [side] (-1 left, 1 right) to the opposite bottom corner. */
    fun stroke(center: Offset, r: Float, side: Float, reach: Float, offset: Float): Pair<Offset, Offset> {
        val tilt = tiltDegrees * PI.toFloat() / HALF_TURN_DEGREES
        val (wide, tall) = proportions
        fun corner(dx: Float, dy: Float) = Offset(
            center.x + r * (reach * wide * dx + offset) * cos(tilt) - r * reach * tall * dy * sin(tilt),
            center.y + r * (reach * wide * dx + offset) * sin(tilt) + r * reach * tall * dy * cos(tilt),
        )
        return corner(side, -1f) to corner(-side, 1f)
    }
}

/** Straight; tilted with a shorter second stroke; bowed both ways, crossing off center; wide and flat with a flick. */
private val X_SHAPES = listOf(
    XShape(tiltDegrees = 0f, first = XStroke(1f), second = XStroke(1f)),
    XShape(tiltDegrees = 8f, first = XStroke(1f, bow = 0.11f), second = XStroke(0.92f)),
    XShape(tiltDegrees = -8f, first = XStroke(0.95f, bow = 0.1f), second = XStroke(0.92f, bow = -0.1f), shift = 0.05f),
    XShape(
        tiltDegrees = 3f,
        first = XStroke(1.06f, bow = 0.05f),
        second = XStroke(1.05f, bow = 0.05f),
        proportions = 1.06f to 0.94f,
        flick = true,
    ),
)

/** One radius of an O, as a share of the mark's: [scale], growing by [grow] along the way, bulging by [bulge]. */
private class ORadius(val scale: Float = 1f, val grow: Float = 0f, val bulge: Float = 0f)

/** One of the four hand-drawn O's: how many [turns] from [startDegrees], its [tilt] (radians), and its radii. */
private class OShape(
    val turns: Float,
    val startDegrees: Float,
    val tilt: Float = 0f,
    val across: ORadius = ORadius(),
    val down: ORadius = ORadius(),
)

/** An overshooting loop, a tilted open oval, a bit-more-than-once spiral, a lopsided egg. */
private val O_SHAPES = listOf(
    OShape(turns = 1.08f, startDegrees = -90f),
    OShape(turns = 0.95f, startDegrees = -60f, tilt = 0.35f, across = ORadius(1.05f), down = ORadius(0.82f)),
    OShape(
        turns = 1.25f,
        startDegrees = -110f,
        across = ORadius(1.04f, grow = -0.12f),
        down = ORadius(1.04f, grow = -0.12f),
    ),
    OShape(turns = 1.04f, startDegrees = -30f, tilt = -0.2f, across = ORadius(bulge = 0.12f), down = ORadius(0.95f)),
)

/** A pen stroke along [line], bowed sideways by [bow], with a slight waver; [flick] hooks its end a little. */
private fun stroke(line: Pair<Offset, Offset>, bow: Float, seed: Int, progress: Float, flick: Boolean = false): Path {
    val (from, to) = line
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = hypot(dx, dy)
    val nx = -dy / length
    val ny = dx / length
    return traced(progress) { t ->
        val side = bow * sin(t * PI.toFloat()) + length * STROKE_WOBBLE * sin(t * PI.toFloat() * STROKE_WAVES + seed)
        val hook = if (flick && t > FLICK_FROM) (t - FLICK_FROM) / (1 - FLICK_FROM) * length * FLICK_SIZE else 0f
        Offset(from.x + dx * t + nx * (side + hook), from.y + dy * t + ny * (side + hook))
    }
}

private fun loop(center: Offset, r: Float, seed: Int, shape: OShape, progress: Float): Path {
    val start = shape.startDegrees * PI.toFloat() / HALF_TURN_DEGREES
    return traced(progress) { t ->
        val a = start + 2 * PI.toFloat() * shape.turns * t
        val wobble = 1 + O_WOBBLE * sin(a * O_WAVES + seed)
        val rx = r * (shape.across.scale + shape.across.grow * t + shape.across.bulge * cos(a)) * wobble
        val ry = r * (shape.down.scale + shape.down.grow * t + shape.down.bulge * sin(a)) * wobble
        val px = rx * cos(a)
        val py = ry * sin(a)
        Offset(
            center.x + px * cos(shape.tilt) - py * sin(shape.tilt),
            center.y + px * sin(shape.tilt) + py * cos(shape.tilt),
        )
    }
}

/** A path through [point] from t = 0 to [progress], so it draws itself in. */
private fun traced(progress: Float = 1f, point: (Float) -> Offset): Path = Path().apply {
    val steps = (STEPS * progress).toInt().coerceAtLeast(1)
    val first = point(0f)
    moveTo(first.x, first.y)
    for (s in 1..steps) {
        val p = point(progress * s / steps)
        lineTo(p.x, p.y)
    }
}

private const val MARK_RADIUS = 0.28f
private const val MARK_STROKE = 0.09f
private const val STRIKE_STROKE = 0.12f
private const val STRIKE_ALPHA = 0.85f
private const val STRIKE_OVERSHOOT = 0.08f
private const val STRIKE_BOW = 0.12f
private const val GRID_STROKE = 0.008f
private const val GRID_WOBBLE = 0.006f
private const val GRID_WAVES = 3
private const val GRID_PHASE_V = 1.7f
private const val GRID_PHASE_H = 2.3f
private const val HINT_CORNER = 0.18f
private const val HINT_SCALE = 0.9f
private const val STEPS = 40
private const val VARIANT_MIX = 7
private const val STROKE_WOBBLE = 0.018f
private const val STROKE_WAVES = 2
private const val FLICK_FROM = 0.88f
private const val FLICK_SIZE = 0.06f
private const val O_WOBBLE = 0.05f
private const val O_WAVES = 3
private const val HALF_TURN_DEGREES = 180f
