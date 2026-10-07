package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Lines
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** Any sequence of legal moves, on any board, against anyone, keeps the game valid. */
class GameInvariantsPropertyTest {
    private val engine = GameEngine()

    private fun randomGame(mode: GameMode, first: Mark, seed: Long): List<GameSession> {
        val random = Random(seed)
        val sessions = mutableListOf(GameSession(seed, engine.newGame(mode, first)))
        while (!sessions.last().state.isOver) {
            val state = sessions.last().state
            val move = engine.legalMoves(state).random(random)
            sessions += sessions.last().play(move, engine)!!
        }
        return sessions
    }

    @Test
    fun `the board stays valid move after move`() = runTest {
        checkAll(ITERATIONS, Arb.element(GameMode.ALL), Arb.enum<Mark>(), Arb.long()) { mode, first, seed ->
            randomGame(mode, first, seed).forEach { session -> session.state.shouldBeValid() }
        }
    }

    @Test
    fun `legal moves are exactly the moves the engine accepts`() = runTest {
        checkAll(ITERATIONS, Arb.element(GameMode.ALL), Arb.enum<Mark>(), Arb.long()) { mode, first, seed ->
            randomGame(mode, first, seed).forEach { session ->
                val state = session.state
                val legal = engine.legalMoves(state).toSet()
                state.board.cells.indices.forEach { cell ->
                    engine.isLegal(state, Move.Place(cell)) shouldBe (Move.Place(cell) in legal)
                }
            }
        }
    }

    @Test
    fun `undoing everything and redoing it all comes back to the same game`() = runTest {
        checkAll(ITERATIONS, Arb.element(GameMode.ALL), Arb.enum<Mark>(), Arb.long()) { mode, first, seed ->
            // The last position before the end where undo and redo stop: the player's turn (any turn in 2-player).
            val session = randomGame(mode, first, seed).last { !it.state.isOver && !it.state.isAiTurn }
            var rewound = session
            while (rewound.canUndo) rewound = rewound.undo()!!
            var replayed = rewound
            while (replayed.canRedo) replayed = replayed.redo()!!
            replayed.state shouldBe session.state
        }
    }

    private fun GameState.shouldBeValid() {
        val xs = board.cells.count { it == Mark.X }
        val os = board.cells.count { it == Mark.O }
        // The opener has placed as many marks as the other player, or one more.
        val openers = if (firstMover == Mark.X) xs - os else os - xs
        (openers == 0 || openers == 1) shouldBe true
        (abs(xs - os) <= 1) shouldBe true
        moves shouldBe xs + os
        val lines = Lines.of(mode.size).filter { line -> line.all { board[it] != null && board[it] == board[line[0]] } }
        when (val s = status) {
            GameStatus.Playing -> {
                lines shouldBe emptyList()
                board.isFull shouldBe false
            }
            is GameStatus.Won -> (s.line in lines) shouldBe true
            GameStatus.Draw -> {
                lines shouldBe emptyList()
                board.isFull shouldBe true
            }
        }
    }

    private companion object {
        const val ITERATIONS = 300
    }
}
