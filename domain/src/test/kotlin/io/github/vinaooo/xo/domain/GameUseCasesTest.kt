package io.github.vinaooo.xo.domain

import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.xo.domain.fake.FakeAchievementRepository
import io.github.vinaooo.xo.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.xo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.xo.domain.fake.FakeSeedSource
import io.github.vinaooo.xo.domain.fake.FakeStatsRepository
import io.github.vinaooo.xo.domain.model.Achievement
import io.github.vinaooo.xo.domain.model.Achievements
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.repository.GameSettings
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.domain.usecase.FinishGame
import io.github.vinaooo.xo.domain.usecase.ResumeGame
import io.github.vinaooo.xo.domain.usecase.SaveGame
import io.github.vinaooo.xo.domain.usecase.StartNewGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GameUseCasesTest {
    private val engine = GameEngine()
    private val hard4 = GameMode(BoardSize.FOUR, Opponent.HARD)
    private val savedGames = FakeSavedGameRepository()
    private val gameSettings = FakeGameSettingsRepository(GameSettings(mode = hard4))
    private val stats = FakeStatsRepository()
    private val start = StartNewGame(savedGames, gameSettings, stats, FakeSeedSource(), engine)
    private val achievements = FakeAchievementRepository()
    private var today = LocalDate.of(2026, 10, 9)
    private val finish = FinishGame(savedGames, stats, achievements) { today }

    private fun GameSession.place(vararg cells: Int) = cells.fold(this) { s, cell ->
        s.play(Move.Place(cell), engine)!!
    }

    @Test
    fun `a new game takes Settings' mode, a fresh seed, and is saved`() = runTest {
        val session = start()
        session.state.mode shouldBe hard4
        session.seed shouldBe 1
        session.state.board.emptyCells.size shouldBe 16
        savedGames.saved shouldBe session
    }

    @Test
    fun `the opener alternates from game to game`() = runTest {
        start().state.firstMover shouldBe Mark.X
        start().state.firstMover shouldBe Mark.O
        start().state.firstMover shouldBe Mark.X
    }

    @Test
    fun `a given mode wins over Settings'`() = runTest {
        val twoPlayer = GameMode(BoardSize.THREE, Opponent.TWO_PLAYER)
        start(twoPlayer).state.mode shouldBe twoPlayer
    }

    @Test
    fun `replacing an AI game in progress counts it as a loss, but an untouched or 2-player one doesn't count`() =
        runTest {
            start()
            start()
            stats.stats.value shouldBe emptyMap()
            savedGames.saved = start().place(0)
            start()
            stats.stats.value shouldBe mapOf(hard4.key to GameStats(played = 1))
            savedGames.saved = start(GameMode(BoardSize.THREE, Opponent.TWO_PLAYER)).place(4)
            start()
            stats.stats.value.keys shouldBe setOf(hard4.key)
        }

    @Test
    fun `finishing records the player's win, loss or draw against the AI, and clears the save`() = runTest {
        val easy3 = GameMode(BoardSize.THREE, Opponent.EASY)
        fun game(first: Mark = Mark.X) = GameSession(1, engine.newGame(easy3, first))
        // X (the player) wins on the top row.
        finish(game().place(0, 3, 1, 4, 2))
        stats.stats.value.getValue(easy3.key) shouldBe GameStats(played = 1, won = 1, currentStreak = 1, bestStreak = 1)
        // O (the AI) wins on the top row.
        finish(game(Mark.O).place(0, 3, 1, 4, 2))
        stats.stats.value.getValue(easy3.key) shouldBe GameStats(played = 2, won = 1, bestStreak = 1)
        // X O X / X O O / O X X
        finish(game().place(0, 1, 2, 4, 3, 5, 7, 6, 8))
        stats.stats.value shouldBe mapOf(easy3.key to GameStats(played = 3, won = 1, bestStreak = 1, drawn = 1))
        savedGames.saved.shouldBeNull()
    }

    @Test
    fun `a finished 2-player game is only cleared, and an unfinished game can't be finished`() = runTest {
        val twoPlayer = GameMode(BoardSize.THREE, Opponent.TWO_PLAYER)
        val won = GameSession(1, engine.newGame(twoPlayer, Mark.X)).place(0, 3, 1, 4, 2)
        savedGames.saved = won
        finish(won) shouldBe emptySet()
        stats.stats.value shouldBe emptyMap()
        achievements.current.value.unlocked shouldBe emptySet()
        savedGames.saved.shouldBeNull()
        shouldThrow<IllegalStateException> { finish(GameSession(1, engine.newGame(twoPlayer, Mark.X))) }
    }

    @Test
    fun `save and resume go through the saved game`() = runTest {
        val session = GameSession(9, engine.newGame(hard4, Mark.X)).place(5)
        SaveGame(savedGames)(session)
        ResumeGame(savedGames)() shouldBe session
    }

    @Test
    fun `finishing returns the badges just earned, and marks the day played`() = runTest {
        val easy3 = GameMode(BoardSize.THREE, Opponent.EASY)
        fun game() = GameSession(1, engine.newGame(easy3, Mark.X))
        val first = finish(game().place(0, 3, 1, 4, 2))
        first shouldBe setOf(Achievement.PLAYED_1, Achievement.WON_1, Achievement.WIN_THREE)
        today = today.plusDays(1)
        finish(game().place(0, 3, 1, 4, 2)) shouldBe emptySet()
        achievements.current.value.collected[Achievements.DAYS_PLAYED] shouldBe setOf("2026-10-09", "2026-10-10")
    }
}
