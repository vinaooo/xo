package io.github.vinaooo.xo.feature.game

import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.session.GameSession

data class GameUiState(
    val session: GameSession? = null,
    val settings: AppSettings = AppSettings(),
    /** The cell the hint suggests, while it shows; the second tap on Hint plays it. */
    val hint: Int? = null,
    /** The AI is about to answer: the board takes no taps meanwhile. */
    val aiThinking: Boolean = false,
    val announcement: Announcement? = null,
    /** Counts announcements, so the same one twice in a row is spoken twice. */
    val announcementSequence: Int = 0,
) {
    /** The player may tap the board: a game is on, not over, and it's not the AI's turn. */
    val canPlay: Boolean
        get() = session != null && !aiThinking && !session.state.isOver && !session.state.isAiTurn
}

sealed interface GameIntent {
    data class Place(val cell: Int) : GameIntent

    data object Undo : GameIntent

    data object Redo : GameIntent

    /** Shows the best move; while one shows, plays it. */
    data object Hint : GameIntent

    data object NewGame : GameIntent
}

/** What TalkBack says after something happens. */
sealed interface Announcement {
    data class Placed(val mark: Mark, val cell: Int, val side: Int) : Announcement

    data class Hinted(val cell: Int, val side: Int) : Announcement

    data object Undone : Announcement

    data object Redone : Announcement

    data class Ended(val status: GameStatus, val vsAi: Boolean) : Announcement
}
