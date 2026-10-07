package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.ai.Ai
import io.github.vinaooo.xo.domain.ai.HeuristicAi
import io.github.vinaooo.xo.domain.ai.MinimaxAi
import io.github.vinaooo.xo.domain.ai.RandomAi
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.rules.MoveOutcome
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.random.Random
import org.junit.jupiter.api.Test

/**
 * Whole games: hard never loses on 3×3, beats random play, answers fast. Slow, so mutation testing leaves them out
 * (`pitest.excludedTestClasses`); `AiTest` pins the same code with fast exact checks.
 */
class AiStrengthTest {
    private val engine = GameEngine()

    private fun GameState.place(vararg cells: Int): GameState =
        cells.fold(this) { state, cell -> (engine.apply(state, Move.Place(cell)) as MoveOutcome.Applied).state }

    private fun start(size: BoardSize, first: Mark = Mark.X) =
        engine.newGame(GameMode(size, Opponent.TWO_PLAYER), first)

    @Test
    fun `hard never loses on 3×3, whatever the player does, whoever opens`() {
        var games = 0
        fun explore(state: GameState) {
            when {
                state.isOver -> {
                    games++
                    (state.status as? GameStatus.Won)?.mark shouldNotBe GameMode.HUMAN
                }
                state.toMove != GameMode.HUMAN -> explore(state.place(MinimaxAi.move(state, Random(games))))
                else -> state.board.emptyCells.forEach { explore(state.place(it)) }
            }
        }
        Mark.entries.forEach { first -> explore(engine.newGame(GameMode(BoardSize.THREE, Opponent.HARD), first)) }
        games shouldBeGreaterThanOrEqual 100
    }

    @Test
    fun `hard beats easy on every board and never loses to medium on 3×3`() {
        BoardSize.entries.forEach { size ->
            val results = (0 until GAMES).map { seed -> play(size, MinimaxAi, RandomAi, seed) }
            results.count { it == Result.LOSS } shouldBe 0
            // A random player sometimes stumbles into a draw on 3×3; on the bigger boards the plan asks for 95% wins.
            val minWins = if (size == BoardSize.THREE) GAMES * 9 / 10 else GAMES * 95 / 100
            results.count { it == Result.WIN } shouldBeGreaterThanOrEqual minWins
        }
        (0 until GAMES).map { play(BoardSize.THREE, MinimaxAi, HeuristicAi, it) }.count { it == Result.LOSS } shouldBe 0
    }

    @Test
    fun `hard answers fast enough for a phone`() {
        listOf(BoardSize.FOUR, BoardSize.FIVE).forEach { size ->
            val slowest = (0 until TIMED_GAMES).maxOf { slowestMove(size, Random(it)) }
            println("Hard on $size: slowest move $slowest ms")
            (slowest < MAX_MOVE_MILLIS) shouldBe true
        }
    }

    /** Hard against random moves on [size]: its slowest answer, in milliseconds. */
    private fun slowestMove(size: BoardSize, random: Random): Long {
        var slowest = 0L
        var state = start(size)
        while (!state.isOver) {
            val started = System.nanoTime()
            val cell = MinimaxAi.move(state, random)
            slowest = maxOf(slowest, (System.nanoTime() - started) / NANOS_PER_MILLI)
            state = state.place(cell)
            if (!state.isOver) state = state.place(state.board.emptyCells.random(random))
        }
        return slowest
    }

    private enum class Result { WIN, DRAW, LOSS }

    /** [ai] against [rival], openers alternating by [seed]; the result for [ai]. */
    private fun play(size: BoardSize, ai: Ai, rival: Ai, seed: Int): Result {
        val random = Random(seed)
        val aiMark = if (seed % 2 == 0) Mark.X else Mark.O
        var state = start(size)
        while (!state.isOver) {
            val mover = if (state.toMove == aiMark) ai else rival
            state = state.place(mover.move(state, random))
        }
        return when ((state.status as? GameStatus.Won)?.mark) {
            aiMark -> Result.WIN
            null -> Result.DRAW
            else -> Result.LOSS
        }
    }

    private companion object {
        const val GAMES = 30
        const val TIMED_GAMES = 5
        const val NANOS_PER_MILLI = 1_000_000L

        /** A phone is a few times slower than this JVM; the plan's budget is 300 ms there. */
        const val MAX_MOVE_MILLIS = 300L
    }
}
