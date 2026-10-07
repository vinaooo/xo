package io.github.vinaooo.xo.feature.game.board

/** A square board of [side]×[side] cells drawn [sizePx] wide: which cell a point falls in, and where cells are. */
internal class BoardGeometry(private val side: Int, private val sizePx: Float) {
    val cellPx: Float get() = sizePx / side

    /** The cell under ([x], [y]), or null outside the board. */
    fun cellAt(x: Float, y: Float): Int? {
        val inside = 0f..<sizePx
        return if (x in inside && y in inside) (y / cellPx).toInt() * side + (x / cellPx).toInt() else null
    }

    /** The center of [cell]. */
    fun center(cell: Int): Pair<Float, Float> = (cell % side + HALF) * cellPx to (cell / side + HALF) * cellPx

    private companion object {
        const val HALF = 0.5f
    }
}
