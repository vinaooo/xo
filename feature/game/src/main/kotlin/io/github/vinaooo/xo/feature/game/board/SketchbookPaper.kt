package io.github.vinaooo.xo.feature.game.board

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/**
 * A sketchbook page filling the board (user's choice among four papers): off-white (`surfaceContainerLow`), a soft
 * shadow, and a light paper grain.
 */
internal fun DrawScope.drawSketchbookPaper(colors: ColorScheme) {
    val corner = CornerRadius(size.width * PAPER_CORNER)
    val shift = size.width * SHADOW_OFFSET
    SHADOW_ALPHAS.forEachIndexed { i, alpha ->
        val grow = shift * (i + 1) / 2
        drawRoundRect(
            Color.Black.copy(alpha = alpha),
            Offset(-grow / 2, shift - grow / 2),
            Size(size.width + grow, size.height + grow),
            CornerRadius(corner.x + grow),
        )
    }
    drawRoundRect(colors.surfaceContainerLow, cornerRadius = corner)
    GRAIN.forEachIndexed { batch, points ->
        drawPoints(
            points.map { Offset(it.x * size.width, it.y * size.height) },
            PointMode.Points,
            colors.onSurface.copy(alpha = GRAIN_ALPHAS[batch]),
            strokeWidth = size.width * GRAIN_SIZE,
            cap = StrokeCap.Round,
        )
    }
}

private const val PAPER_CORNER = 0.03f
private const val SHADOW_OFFSET = 0.012f
private val SHADOW_ALPHAS = listOf(0.05f, 0.04f, 0.03f)
private const val GRAIN_SEED = 5
private const val GRAIN_PER_BATCH = 600
private val GRAIN_ALPHAS = listOf(0.015f, 0.03f, 0.045f)
private const val GRAIN_SIZE = 0.005f

/** Grain specks across the page (0..1), in batches of different strength: the same on every frame and every board. */
private val GRAIN: List<List<Offset>> = Random(GRAIN_SEED).let { random ->
    List(GRAIN_ALPHAS.size) { List(GRAIN_PER_BATCH) { Offset(random.nextFloat(), random.nextFloat()) } }
}
