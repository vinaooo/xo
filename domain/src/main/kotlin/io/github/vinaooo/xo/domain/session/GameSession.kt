package io.github.vinaooo.xo.domain.session

import io.github.vinaooo.vinkit.core.GameCodec
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.rules.MoveOutcome
import kotlinx.serialization.Serializable

/**
 * A game being played: its seed (the AI's random choices), the current state, and the states before each move
 * ([undos]) and after each undone one ([redos]). This is what gets saved.
 *
 * Against the AI, undo goes back to the player's previous turn, taking the AI's reply back with the player's move,
 * and redo replays both. A finished game can't be undone: its result is already in the stats. A new move clears redo.
 * [hintsUsed] and [undosUsed] count over the whole game (undo doesn't take them back), for the badges.
 */
@Serializable
data class GameSession(
    val seed: Long,
    val state: GameState,
    val undos: List<GameState> = emptyList(),
    val redos: List<GameState> = emptyList(),
    val hintsUsed: Int = 0,
    val undosUsed: Int = 0,
) {
    val canUndo: Boolean get() = !state.isOver && undos.any(::isPlayersTurn)

    val canRedo: Boolean get() = !state.isOver && redos.any(::isPlayersTurn)

    /** Moves were made and the game isn't over: abandoning it counts as a loss against the AI. */
    val isInProgress: Boolean get() = state.moves > 0 && !state.isOver

    fun play(move: Move, engine: GameEngine): GameSession? = when (val outcome = engine.apply(state, move)) {
        is MoveOutcome.Applied -> copy(state = outcome.state, undos = undos + state, redos = emptyList())
        MoveOutcome.Rejected -> null
    }

    fun undo(): GameSession? {
        if (!canUndo) return null
        var session = this
        do {
            session = session.copy(
                state = session.undos.last(),
                undos = session.undos.dropLast(1),
                redos = session.redos + session.state,
            )
        } while (!isPlayersTurn(session.state))
        return session.copy(undosUsed = undosUsed + 1)
    }

    fun redo(): GameSession? {
        if (!canRedo) return null
        var session = this
        do {
            session = session.copy(
                state = session.redos.last(),
                undos = session.undos + session.state,
                redos = session.redos.dropLast(1),
            )
        } while (!isPlayersTurn(session.state))
        return session
    }

    /** Where undo and redo stop: any turn in 2-player, the player's turn against the AI. */
    private fun isPlayersTurn(state: GameState): Boolean = !state.mode.isVsAi || state.toMove == GameMode.HUMAN

    companion object {
        /** A game's state as short text for bug reports ("State:"), and back for the debug build. */
        val codec = GameCodec(GameState.serializer())
    }
}
