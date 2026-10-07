package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.ai.HeuristicAi
import io.github.vinaooo.xo.domain.ai.MinimaxAi
import io.github.vinaooo.xo.domain.ai.RandomAi
import io.github.vinaooo.xo.domain.ai.aiFor
import io.github.vinaooo.xo.domain.ai.centerCells
import io.github.vinaooo.xo.domain.ai.orderedMoves
import io.github.vinaooo.xo.domain.ai.random
import io.github.vinaooo.xo.domain.ai.winningCells
import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Lines
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.rules.MoveOutcome
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import org.junit.jupiter.api.Test

class AiTest {
    private val engine = GameEngine()

    private fun GameState.place(vararg cells: Int): GameState =
        cells.fold(this) { state, cell -> (engine.apply(state, Move.Place(cell)) as MoveOutcome.Applied).state }

    private fun start(size: BoardSize, first: Mark = Mark.X) =
        engine.newGame(GameMode(size, Opponent.TWO_PLAYER), first)

    @Test
    fun `each opponent has its AI, 2-player none`() {
        aiFor(Opponent.EASY) shouldBe RandomAi
        aiFor(Opponent.MEDIUM) shouldBe HeuristicAi
        aiFor(Opponent.HARD) shouldBe MinimaxAi
        aiFor(Opponent.TWO_PLAYER).shouldBeNull()
    }

    @Test
    fun `easy takes an empty cell, the same one for the same seed`() {
        val state = start(BoardSize.THREE).place(0, 4, 8)
        repeat(20) { RandomAi.move(state, Random(it)) shouldBeIn state.board.emptyCells }
        RandomAi.move(state, Random(5)) shouldBe RandomAi.move(state, Random(5))
    }

    @Test
    fun `a session's random choices depend only on its seed and position`() {
        val session = GameSession(42, start(BoardSize.FIVE))
        session.random().nextInt() shouldBe GameSession(42, start(BoardSize.FIVE)).random().nextInt()
        session.random().nextInt() shouldNotBe GameSession(43, start(BoardSize.FIVE)).random().nextInt()
    }

    @Test
    fun `medium and hard win when they can, and block when they must`() {
        // X X . / O O . / . . . with X to move: 2 wins.
        val canWin = start(BoardSize.THREE).place(0, 3, 1, 4)
        listOf(HeuristicAi, MinimaxAi).forEach { it.move(canWin, Random(0)) shouldBe 2 }
        // X X . / O . . / . . . with O to move: 2 blocks.
        val mustBlock = start(BoardSize.THREE).place(0, 3, 1)
        listOf(HeuristicAi, MinimaxAi).forEach { it.move(mustBlock, Random(0)) shouldBe 2 }
    }

    @Test
    fun `medium takes the center when nothing is urgent`() {
        HeuristicAi.move(start(BoardSize.THREE), Random(0)) shouldBe 4
        HeuristicAi.move(start(BoardSize.FOUR), Random(0)) shouldBeIn listOf(5, 6, 9, 10)
        HeuristicAi.move(start(BoardSize.FIVE).place(12), Random(0)) shouldBeIn start(BoardSize.FIVE).board.emptyCells
    }

    @Test
    fun `hard's search finds the true value of every 3×3 position`() {
        val seen = HashSet<GameState>()
        fun visit(state: GameState) {
            if (state.isOver || !seen.add(state)) return
            MinimaxAi.value(state.board, state.toMove) shouldBe reference(state, 0)
            state.board.emptyCells.forEach { visit(state.place(it)) }
        }
        Mark.entries.forEach { visit(start(BoardSize.THREE, it)) }
        seen.size shouldBeGreaterThanOrEqual 4_000
    }

    /** Plain negamax, no pruning or cache: what the search must agree with. */
    private fun reference(state: GameState, depth: Int): Int = when (val status = state.status) {
        is GameStatus.Won -> if (status.mark == state.toMove) MinimaxAi.WIN - depth else -(MinimaxAi.WIN - depth)
        GameStatus.Draw -> 0
        GameStatus.Playing -> state.board.emptyCells.maxOf { -reference(state.place(it), depth + 1) }
    }

    @Test
    fun `open lines count by how full they are, for and against`() {
        // X . . / . . . / . . . : X has 3 lines with one mark (row, column, diagonal).
        val oneX = start(BoardSize.THREE).place(0)
        MinimaxAi.potential(oneX.board, Mark.X) shouldBe 3
        MinimaxAi.potential(oneX.board, Mark.O) shouldBe -3
        // X X . / O . . : row 0 has 2 X (10); column 0 is blocked; diagonal 0-4-8 has 1 X; column 1 has 1 X;
        // diagonal 2-4-6 is empty; O has row 1 and its diagonal-free column 0 is blocked: 1 line with 1 O.
        val mixed = start(BoardSize.THREE).place(0, 3, 1)
        MinimaxAi.potential(mixed.board, Mark.X) shouldBe 10 + 1 + 1 - 1
    }

    @Test
    fun `hard looks further ahead as the board fills`() {
        MinimaxAi.depthFor(start(BoardSize.THREE).board) shouldBe 9
        MinimaxAi.depthFor(start(BoardSize.FOUR).board) shouldBe 6
        MinimaxAi.depthFor(start(BoardSize.FOUR).place(0, 1, 2, 3, 4, 5).board) shouldBe 6
        MinimaxAi.depthFor(start(BoardSize.FOUR).place(0, 1, 2, 3, 4, 5, 6).board) shouldBe 16
        MinimaxAi.depthFor(start(BoardSize.FIVE).board) shouldBe 4
        MinimaxAi.depthFor(start(BoardSize.FIVE).place(*IntArray(15) { it }).board) shouldBe 4
        MinimaxAi.depthFor(start(BoardSize.FIVE).place(*IntArray(16) { it }).board) shouldBe 6
    }

    @Test
    fun `winning cells need the rest of the line to be the mark's, center cells sit in the middle`() {
        // X X O on row 0: no win there; X . X on row 2: 7 wins.
        val board = start(BoardSize.THREE).place(0, 2, 1, 4, 6, 3, 8).board
        winningCells(board, Mark.X) shouldBe listOf(7)
        winningCells(board, Mark.O) shouldBe listOf(5)
        centerCells(start(BoardSize.THREE).board) shouldBe listOf(4)
        centerCells(start(BoardSize.FOUR).board) shouldBe listOf(5, 6, 9, 10)
        centerCells(start(BoardSize.FIVE).board) shouldBe listOf(12)
    }

    @Test
    fun `a saved game replays the same AI choices, so the random source never changes`() {
        val session = GameSession(42, start(BoardSize.THREE).place(4))
        session.random().nextInt() shouldBe Random(42L * 31 + 1).nextInt()
    }

    @Test
    fun `on every 3×3 position, hard plays a move of the best value`() {
        val seen = HashSet<GameState>()
        fun visit(state: GameState) {
            if (state.isOver || !seen.add(state)) return
            val best = state.board.emptyCells.maxOf { -MinimaxAi.value(state.place(it).board, state.toMove.other) }
            val chosen = MinimaxAi.move(state, Random(seen.size))
            -MinimaxAi.value(state.place(chosen).board, state.toMove.other) shouldBe best
            state.board.emptyCells.forEach { visit(state.place(it)) }
        }
        Mark.entries.forEach { visit(start(BoardSize.THREE, it)) }
    }

    @Test
    fun `hard looks at winning cells first, then blocks, then the cells on the most open lines`() {
        // X X . / O O . / . . . with X to move: 2 wins, 5 blocks; then 8 (open diagonal and column), then the rest.
        val board = start(BoardSize.THREE).place(0, 3, 1, 4).board
        val order = orderedMoves(board, Mark.X)
        order.take(2) shouldBe listOf(2, 5)
        order[2] shouldBe 8
        order.toSet() shouldBe board.emptyCells.toSet()
        // On an empty board, the center (4 lines) comes before a corner (3) before an edge (2).
        orderedMoves(start(BoardSize.THREE).board, Mark.O).let {
            it.indexOf(4) < it.indexOf(0) &&
                it.indexOf(0) < it.indexOf(1)
        } shouldBe
            true
    }

    @Test
    fun `with nothing to win or lose yet, hard takes a center cell on the bigger boards`() {
        listOf(BoardSize.FOUR, BoardSize.FIVE).forEach { size ->
            val empty = start(size)
            MinimaxAi.move(empty, Random(0)) shouldBeIn centerCells(empty.board)
        }
    }

    @Test
    fun `winning cells and open lines agree with a plain count on random positions`() {
        val random = Random(1)
        repeat(POSITIONS) {
            val size = BoardSize.entries.random(random)
            var state = start(size, Mark.entries.random(random))
            repeat(random.nextInt(size.cells - 1)) {
                if (!state.isOver) state = state.place(state.board.emptyCells.random(random))
            }
            Mark.entries.forEach { mark ->
                val brute = state.board.emptyCells.filter { state.board.place(it, mark).winningLineThrough(it) != null }
                winningCells(state.board, mark).sorted() shouldBe brute
                MinimaxAi.potential(state.board, mark) shouldBe plainPotential(state.board, mark)
            }
        }
    }

    /** Each line with only [mark]'s marks adds 1, 10, 100 or 1000 by how many; the opponent's subtract. */
    private fun plainPotential(board: Board, mark: Mark): Int = Lines.of(board.size).sumOf { line ->
        val marks = line.mapNotNull { board[it] }.toSet()
        val count = line.count { board[it] != null }
        when {
            marks.size != 1 -> 0
            marks.single() == mark -> WEIGHTS[count]
            else -> -WEIGHTS[count]
        }
    }

    private companion object {
        const val POSITIONS = 300
        val WEIGHTS = intArrayOf(0, 1, 10, 100, 1_000)
    }
}
