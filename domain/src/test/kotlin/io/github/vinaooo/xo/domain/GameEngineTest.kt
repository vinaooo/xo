package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.rules.MoveOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class GameEngineTest {
    private val engine = GameEngine()
    private val twoPlayer3 = GameMode(BoardSize.THREE, Opponent.TWO_PLAYER)

    private fun play(mode: GameMode, vararg cells: Int, first: Mark = Mark.X): GameState =
        cells.fold(engine.newGame(mode, first)) { state, cell ->
            (engine.apply(state, Move.Place(cell)) as MoveOutcome.Applied).state
        }

    @Test
    fun `a new game is empty, with the opener to move`() {
        val state = engine.newGame(twoPlayer3, Mark.O)
        state.board.emptyCells.size shouldBe 9
        state.toMove shouldBe Mark.O
        state.firstMover shouldBe Mark.O
        state.status shouldBe GameStatus.Playing
    }

    @Test
    fun `marks alternate and count as moves`() {
        val state = play(twoPlayer3, 4, 0)
        state.board[4] shouldBe Mark.X
        state.board[0] shouldBe Mark.O
        state.toMove shouldBe Mark.X
        state.moves shouldBe 2
    }

    @Test
    fun `a taken cell, a cell off the board and any move after the end are rejected`() {
        val state = play(twoPlayer3, 4)
        engine.apply(state, Move.Place(4)) shouldBe MoveOutcome.Rejected
        engine.apply(state, Move.Place(9)) shouldBe MoveOutcome.Rejected
        engine.apply(state, Move.Place(-1)) shouldBe MoveOutcome.Rejected
        val won = play(twoPlayer3, 0, 3, 1, 4, 2)
        engine.apply(won, Move.Place(8)) shouldBe MoveOutcome.Rejected
        engine.legalMoves(won) shouldBe emptyList()
    }

    @Test
    fun `a full row, column or diagonal wins with its line`() {
        play(twoPlayer3, 0, 3, 1, 4, 2).status shouldBe GameStatus.Won(Mark.X, listOf(0, 1, 2))
        play(twoPlayer3, 0, 1, 3, 2, 6).status shouldBe GameStatus.Won(Mark.X, listOf(0, 3, 6))
        play(twoPlayer3, 1, 0, 2, 4, 3, 8).status shouldBe GameStatus.Won(Mark.O, listOf(0, 4, 8))
        play(twoPlayer3, 0, 2, 1, 4, 3, 6).status shouldBe GameStatus.Won(Mark.O, listOf(2, 4, 6))
    }

    @Test
    fun `a full board without a line is a draw`() {
        // X O X / X O O / O X X
        play(twoPlayer3, 0, 1, 2, 4, 3, 5, 7, 6, 8).status shouldBe GameStatus.Draw
    }

    @Test
    fun `on 5×5 four in a row wins, anywhere along the line`() {
        val five = GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER)
        play(five, 1, 20, 2, 21, 3, 22, 4).status shouldBe GameStatus.Won(Mark.X, listOf(1, 2, 3, 4))
        play(five, 6, 0, 12, 1, 18, 2, 24).status.shouldBeInstanceOf<GameStatus.Won>().line shouldBe
            listOf(6, 12, 18, 24)
    }

    @Test
    fun `on 4×4 three in a row isn't enough`() {
        val four = GameMode(BoardSize.FOUR, Opponent.TWO_PLAYER)
        play(four, 0, 15, 1, 14, 2).status shouldBe GameStatus.Playing
        play(four, 0, 15, 1, 14, 2, 13, 3).status shouldBe GameStatus.Won(Mark.X, listOf(0, 1, 2, 3))
    }

    @Test
    fun `against the AI, it is the AI's turn when O is to move`() {
        val vsAi = GameMode(BoardSize.THREE, Opponent.HARD)
        engine.newGame(vsAi, Mark.O).isAiTurn shouldBe true
        engine.newGame(vsAi, Mark.X).isAiTurn shouldBe false
        engine.newGame(twoPlayer3, Mark.O).isAiTurn shouldBe false
    }
}
