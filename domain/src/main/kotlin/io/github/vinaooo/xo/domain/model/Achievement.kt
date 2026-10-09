package io.github.vinaooo.xo.domain.model

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.xo.domain.session.GameSession
import java.time.LocalDate

/**
 * A badge, earned in games against the AI. Its name is its key in storage (vinkit's `AchievementProgress`): never
 * rename one after release. A badge on a [ladder] is earned once that count reaches [count].
 */
enum class Achievement(val ladder: Ladder? = null, val count: Int = 0) {
    PLAYED_1(Ladder.PLAYED, 1),
    PLAYED_10(Ladder.PLAYED, 10),
    PLAYED_50(Ladder.PLAYED, 50),
    PLAYED_100(Ladder.PLAYED, 100),
    PLAYED_500(Ladder.PLAYED, 500),
    WON_1(Ladder.WON, 1),
    WON_10(Ladder.WON, 10),
    WON_50(Ladder.WON, 50),
    WON_100(Ladder.WON, 100),
    WON_500(Ladder.WON, 500),
    STREAK_3(Ladder.STREAK, 3),
    STREAK_5(Ladder.STREAK, 5),
    STREAK_10(Ladder.STREAK, 10),
    DAYS_3(Ladder.DAYS, 3),
    DAYS_7(Ladder.DAYS, 7),
    DAYS_30(Ladder.DAYS, 30),
    WIN_THREE,
    WIN_FOUR,
    WIN_FIVE,
    WIN_EVERY_SIZE,
    BEAT_HARD,
    DRAW_HARD,
    QUICK_WIN,
    WIN_SECOND,
    NO_HINTS,
    NO_UNDO,
}

/** What a ladder of badges counts. */
enum class Ladder {
    /** Games played against the AI, in every mode. */
    PLAYED,

    /** Games won against the AI, in every mode. */
    WON,

    /** The best run of wins in a row in one mode (a draw ends it). */
    STREAK,

    /** Days in a row with a game against the AI finished. */
    DAYS,
}

/** The badges earned that this version knows; keys a newer version wrote are skipped. */
val AchievementProgress.badges: Set<Achievement>
    get() = Achievement.entries.filter { it.name in unlocked }.toSet()

/** When each badge is earned. Badges read the stats, so what a player already did before badges counts too. */
object Achievements {
    /** The collected set of days a game against the AI ended (ISO dates, local time): never rename. */
    const val DAYS_PLAYED = "days_played"

    private val SIZE_WINS = mapOf(
        BoardSize.THREE to Achievement.WIN_THREE,
        BoardSize.FOUR to Achievement.WIN_FOUR,
        BoardSize.FIVE to Achievement.WIN_FIVE,
    )

    /**
     * [progress] after [ended], a finished game against the AI, played [today]; [stats] holds every AI mode's stats
     * with that game in them. What this version doesn't know is kept.
     */
    fun after(
        progress: AchievementProgress,
        stats: Map<GameMode, GameStats>,
        ended: GameSession,
        today: LocalDate,
    ): AchievementProgress {
        // ponytail: every day played is kept (a few KB a year); prune to the last DAYS_30 days if it ever matters.
        val days = progress.collected[DAYS_PLAYED].orEmpty() + today.toString()
        val earned = earned(stats, dayStreak(days, today), ended)
        return AchievementProgress(
            progress.unlocked + earned.map { it.name },
            progress.collected + (DAYS_PLAYED to days),
        )
    }

    fun earned(stats: Map<GameMode, GameStats>, dayStreak: Int, ended: GameSession): Set<Achievement> = buildSet {
        val all = stats.values
        val counts = mapOf(
            Ladder.PLAYED to all.sumOf { it.played },
            Ladder.WON to all.sumOf { it.won },
            Ladder.STREAK to (all.maxOfOrNull { it.bestStreak } ?: 0),
            Ladder.DAYS to dayStreak,
        )
        addAll(Achievement.entries.filter { badge -> badge.ladder?.let { counts.getValue(it) >= badge.count } == true })
        val won = stats.filterValues { it.won > 0 }.keys
        val sizesWon = won.map { it.size }.toSet()
        sizesWon.forEach { add(SIZE_WINS.getValue(it)) }
        if (sizesWon.containsAll(BoardSize.entries)) add(Achievement.WIN_EVERY_SIZE)
        if (won.any { it.opponent == Opponent.HARD }) add(Achievement.BEAT_HARD)
        if ((stats[GameMode(BoardSize.THREE, Opponent.HARD)]?.drawn ?: 0) > 0) add(Achievement.DRAW_HARD)
        addAll(winBadges(ended))
    }

    /** The days in a row played up to [today], from the ISO dates in [days]. */
    fun dayStreak(days: Set<String>, today: LocalDate): Int =
        generateSequence(today) { it.minusDays(1) }.takeWhile { it.toString() in days }.count()

    private fun winBadges(session: GameSession): Set<Achievement> = buildSet {
        val state = session.state
        if ((state.status as? GameStatus.Won)?.mark != GameMode.HUMAN) return@buildSet
        // Easy plays at random: these only count against Medium or Hard.
        if (state.mode.opponent == Opponent.EASY) return@buildSet
        if (state.board.cells.count { it == GameMode.HUMAN } == state.mode.size.lineLength) add(Achievement.QUICK_WIN)
        if (state.firstMover != GameMode.HUMAN) add(Achievement.WIN_SECOND)
        if (session.hintsUsed == 0) add(Achievement.NO_HINTS)
        if (session.undosUsed == 0) add(Achievement.NO_UNDO)
    }
}
