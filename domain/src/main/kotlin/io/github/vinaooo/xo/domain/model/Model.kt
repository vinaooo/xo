package io.github.vinaooo.xo.domain.model

import kotlinx.serialization.Serializable

enum class Mark {
    X,
    O,
    ;

    val other: Mark get() = if (this == X) O else X
}

/** A board's side and how many marks in a row win on it. */
enum class BoardSize(val side: Int, val lineLength: Int) {
    THREE(3, 3),
    FOUR(4, 4),
    FIVE(5, 4),
    ;

    val cells: Int get() = side * side
}

enum class Opponent {
    EASY,
    MEDIUM,
    HARD,
    TWO_PLAYER,
}

/** A board size against an opponent. It travels with the game: a resumed game keeps it whatever Settings say now. */
@Serializable
data class GameMode(val size: BoardSize, val opponent: Opponent) {
    val isVsAi: Boolean get() = opponent != Opponent.TWO_PLAYER

    /** The key vinkit's stats store this mode under. */
    val key: String get() = "${size.name}_${opponent.name}"

    companion object {
        val DEFAULT = GameMode(BoardSize.THREE, Opponent.MEDIUM)

        /** The player's mark against the AI. */
        val HUMAN = Mark.X

        val ALL: List<GameMode> = BoardSize.entries.flatMap { size -> Opponent.entries.map { GameMode(size, it) } }

        fun fromKey(key: String): GameMode? = ALL.firstOrNull { it.key == key }
    }
}
