package io.github.vinaooo.xo.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface GameStatus {
    @Serializable
    data object Playing : GameStatus

    @Serializable
    data class Won(val mark: Mark, val line: List<Int>) : GameStatus

    @Serializable
    data object Draw : GameStatus
}

/** A game: the board, its mode, whose turn it is, who opened, and how it stands. */
@Serializable
data class GameState(
    val board: Board,
    val mode: GameMode,
    val toMove: Mark,
    val firstMover: Mark,
    val status: GameStatus = GameStatus.Playing,
    val moves: Int = 0,
) {
    val isOver: Boolean get() = status != GameStatus.Playing

    /** Against the AI: it's the AI's turn. */
    val isAiTurn: Boolean get() = mode.isVsAi && !isOver && toMove != GameMode.HUMAN
}

sealed interface Move {
    data class Place(val cell: Int) : Move
}
