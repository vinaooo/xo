package io.github.vinaooo.xo.domain

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.xo.domain.model.Achievement
import io.github.vinaooo.xo.domain.model.Achievements
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.model.badges
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.junit.jupiter.api.Test

class AchievementsTest {
    private val engine = GameEngine()
    private val easy3 = GameMode(BoardSize.THREE, Opponent.EASY)
    private val medium3 = GameMode(BoardSize.THREE, Opponent.MEDIUM)
    private val hard3 = GameMode(BoardSize.THREE, Opponent.HARD)
    private val hard4 = GameMode(BoardSize.FOUR, Opponent.HARD)
    private val easy5 = GameMode(BoardSize.FIVE, Opponent.EASY)
    private val today = LocalDate.of(2026, 10, 9)

    private fun game(mode: GameMode = easy3, first: Mark = Mark.X, vararg cells: Int) =
        cells.fold(GameSession(1, engine.newGame(mode, first))) { s, cell -> s.play(Move.Place(cell), engine)!! }

    /** X (the player) wins the top row of 3×3 with three marks. */
    private val quickWin = game(medium3, Mark.X, 0, 3, 1, 4, 2)

    /** O (the AI) wins the top row. */
    private val loss = game(easy3, Mark.O, 0, 3, 1, 4, 2)

    @Test
    fun `ladders count games played and won in every mode, the best streak and the days`() {
        val stats = mapOf(easy3 to GameStats(played = 40, won = 9, bestStreak = 5), hard4 to GameStats(played = 10))
        Achievements.earned(stats, dayStreak = 7, ended = loss) shouldBe setOf(
            Achievement.PLAYED_1, Achievement.PLAYED_10, Achievement.PLAYED_50,
            Achievement.WON_1, Achievement.STREAK_3, Achievement.STREAK_5,
            Achievement.DAYS_3, Achievement.DAYS_7, Achievement.WIN_THREE,
        )
    }

    @Test
    fun `board and hard badges come from the stats`() {
        val won = GameStats(played = 1, won = 1)
        Achievements.earned(mapOf(easy3 to won, hard4 to won), 0, loss) shouldContainAll
            listOf(Achievement.WIN_THREE, Achievement.WIN_FOUR, Achievement.BEAT_HARD)
        Achievements.earned(mapOf(easy3 to won, hard4 to won, easy5 to won), 0, loss) shouldContainAll
            listOf(Achievement.WIN_FIVE, Achievement.WIN_EVERY_SIZE)
        Achievements.earned(mapOf(hard3 to GameStats(played = 1, drawn = 1)), 0, loss) shouldBe
            setOf(Achievement.PLAYED_1, Achievement.DRAW_HARD)
        Achievements.earned(mapOf(easy3 to GameStats(played = 1, drawn = 1)), 0, loss) shouldBe
            setOf(Achievement.PLAYED_1)
    }

    @Test
    fun `a win against medium or hard earns quick, second, no hints and no undo badges by how it was played`() {
        Achievements.earned(emptyMap(), 0, quickWin) shouldBe
            setOf(Achievement.QUICK_WIN, Achievement.NO_HINTS, Achievement.NO_UNDO)
        // The AI opens at 8; X wins the top row with three marks after it.
        val second = game(medium3, Mark.O, 8, 0, 3, 1, 4, 2).copy(hintsUsed = 1, undosUsed = 2)
        Achievements.earned(emptyMap(), 0, second) shouldBe setOf(Achievement.QUICK_WIN, Achievement.WIN_SECOND)
        // X wins the top row with a fourth mark already down.
        val slow = game(medium3, Mark.X, 8, 3, 0, 4, 1, 6, 2)
        Achievements.earned(emptyMap(), 0, slow) shouldNotContainAnyOf listOf(Achievement.QUICK_WIN)
        Achievements.earned(emptyMap(), 0, loss) shouldBe emptySet()
        Achievements.earned(emptyMap(), 0, game(easy3, Mark.X, 0, 3, 1, 4, 2)) shouldBe emptySet()
    }

    @Test
    fun `days in a row count back from today`() {
        Achievements.dayStreak(setOf("2026-10-09", "2026-10-08", "2026-10-06"), today) shouldBe 2
        Achievements.dayStreak(setOf("2026-10-08"), today) shouldBe 0
    }

    @Test
    fun `after adds the day and the badges, and keeps what this version doesn't know`() {
        val before = AchievementProgress(
            unlocked = setOf("FROM_THE_FUTURE"),
            collected = mapOf(Achievements.DAYS_PLAYED to setOf("2026-10-08"), "other" to setOf("a")),
        )
        val after = Achievements.after(before, emptyMap(), quickWin, today)
        after.unlocked shouldBe setOf("FROM_THE_FUTURE", "QUICK_WIN", "NO_HINTS", "NO_UNDO")
        after.collected shouldBe mapOf(
            Achievements.DAYS_PLAYED to setOf("2026-10-08", "2026-10-09"),
            "other" to setOf("a"),
        )
        after.badges shouldBe setOf(Achievement.QUICK_WIN, Achievement.NO_HINTS, Achievement.NO_UNDO)
    }
}
