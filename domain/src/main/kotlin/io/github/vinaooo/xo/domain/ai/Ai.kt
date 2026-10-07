package io.github.vinaooo.xo.domain.ai

import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.session.GameSession
import kotlin.random.Random

/** Picks the cell for the side to move in [state]; [random] breaks ties, so a seed replays a game exactly. */
fun interface Ai {
    fun move(state: GameState, random: Random): Int
}

/** The AI playing [opponent]; null for 2-player. */
fun aiFor(opponent: Opponent): Ai? = when (opponent) {
    Opponent.EASY -> RandomAi
    Opponent.MEDIUM -> HeuristicAi
    Opponent.HARD -> MinimaxAi
    Opponent.TWO_PLAYER -> null
}

/** The random choices for the position after [GameSession.state]'s moves: the same game always gets the same ones. */
fun GameSession.random(): Random = Random(seed * RANDOM_MIX + state.moves)

private const val RANDOM_MIX = 31

/** Easy: any empty cell. */
object RandomAi : Ai {
    override fun move(state: GameState, random: Random): Int = state.board.emptyCells.random(random)
}
