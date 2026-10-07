package io.github.vinaooo.xo.domain.model

import kotlinx.serialization.Serializable

/** The cells, row by row from the top left, each empty (null) or marked. */
@Serializable
data class Board(val size: BoardSize, val cells: List<Mark?>) {
    init {
        require(cells.size == size.cells) {
            "A ${size.side}×${size.side} board has ${size.cells} cells, not ${cells.size}"
        }
    }

    val emptyCells: List<Int> get() = cells.indices.filter { cells[it] == null }

    val isFull: Boolean get() = cells.none { it == null }

    operator fun get(cell: Int): Mark? = cells[cell]

    fun place(cell: Int, mark: Mark): Board = copy(cells = cells.toMutableList().also { it[cell] = mark })

    fun row(cell: Int): Int = cell / size.side

    fun column(cell: Int): Int = cell % size.side

    /** A full line of [cell]'s mark through [cell], if its mark completed one. */
    fun winningLineThrough(cell: Int): List<Int>? {
        val mark = cells[cell] ?: return null
        return Lines.through(size, cell).firstOrNull { line -> line.all { cells[it] == mark } }
    }

    companion object {
        fun empty(size: BoardSize) = Board(size, List(size.cells) { null })
    }
}

/** Every run of [BoardSize.lineLength] cells in a row, column or diagonal: the ways to win. */
object Lines {
    private val all: Map<BoardSize, List<List<Int>>> = BoardSize.entries.associateWith(::linesOf)
    private val byCell: Map<BoardSize, List<List<List<Int>>>> = BoardSize.entries.associateWith { size ->
        List(size.cells) { cell -> all.getValue(size).filter { cell in it } }
    }

    fun of(size: BoardSize): List<List<Int>> = all.getValue(size)

    fun through(size: BoardSize, cell: Int): List<List<Int>> = byCell.getValue(size)[cell]

    private fun linesOf(size: BoardSize): List<List<Int>> {
        val n = size.side
        val k = size.lineLength
        // Steps across a row, down a column, and along both diagonals.
        val directions = listOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)
        return buildList {
            for (row in 0 until n) {
                for (column in 0 until n) {
                    for ((dr, dc) in directions) {
                        val endRow = row + dr * (k - 1)
                        val endColumn = column + dc * (k - 1)
                        if (endRow in 0 until n && endColumn in 0 until n) {
                            add(List(k) { step -> (row + dr * step) * n + column + dc * step })
                        }
                    }
                }
            }
        }
    }
}
