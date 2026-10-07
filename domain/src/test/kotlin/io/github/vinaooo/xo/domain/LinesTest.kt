package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.Lines
import io.github.vinaooo.xo.domain.model.Mark
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LinesTest {
    @Test
    fun `every board has its ways to win`() {
        // 3 rows + 3 columns + 2 diagonals.
        Lines.of(BoardSize.THREE).size shouldBe 8
        // 4 rows + 4 columns + 2 diagonals.
        Lines.of(BoardSize.FOUR).size shouldBe 10
        // 4 in a row on 5×5: 2 per row and column, 4 per diagonal direction.
        Lines.of(BoardSize.FIVE).size shouldBe 28
    }

    @Test
    fun `a line is lineLength cells in a straight, unbroken run`() {
        BoardSize.entries.forEach { size ->
            Lines.of(size).forEach { line ->
                line.size shouldBe size.lineLength
                val steps = line.zipWithNext { a, b -> b - a }.toSet()
                steps.size shouldBe 1
                // Along a row, down a column, or along either diagonal.
                (steps.single() in setOf(1, size.side, size.side + 1, size.side - 1)) shouldBe true
            }
        }
    }

    @Test
    fun `cells are numbered row by row, and a board has exactly its cells`() {
        val board = Board.empty(BoardSize.FIVE)
        board.row(13) shouldBe 2
        board.column(13) shouldBe 3
        board.place(13, Mark.O)[13] shouldBe Mark.O
        board.emptyCells.size shouldBe 25
        shouldThrow<IllegalArgumentException> { Board(BoardSize.THREE, List(8) { null }) }
        board.isFull shouldBe false
        Board(BoardSize.THREE, List(9) { Mark.X }).isFull shouldBe true
        // An empty cell completes nothing.
        board.winningLineThrough(13) shouldBe null
    }

    @Test
    fun `the lines through a cell are exactly those holding it`() {
        Lines.through(BoardSize.THREE, 4).size shouldBe 4
        Lines.through(BoardSize.THREE, 0).size shouldBe 3
        Lines.through(BoardSize.FIVE, 12) shouldContainAll listOf(listOf(10, 11, 12, 13), listOf(0, 6, 12, 18))
    }
}
