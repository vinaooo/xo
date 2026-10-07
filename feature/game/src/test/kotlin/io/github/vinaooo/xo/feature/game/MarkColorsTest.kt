package io.github.vinaooo.xo.feature.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.paletteScheme
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import org.junit.jupiter.api.Test

/** X (primary) and O (tertiary) stay readable on the board (surface) in every palette, light and dark. */
class MarkColorsTest {
    @Test
    fun `marks keep 3 to 1 contrast against the board`() {
        ThemeColor.entries.forEach { color ->
            listOf(false, true).forEach { dark ->
                val scheme = paletteScheme(color, dark)
                contrast(scheme.primary, scheme.surface) shouldBeGreaterThanOrEqual MIN_CONTRAST
                contrast(scheme.tertiary, scheme.surface) shouldBeGreaterThanOrEqual MIN_CONTRAST
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + OFFSET) / (dark + OFFSET).toDouble()
    }

    private companion object {
        const val MIN_CONTRAST = 3.0
        const val OFFSET = 0.05f
    }
}
