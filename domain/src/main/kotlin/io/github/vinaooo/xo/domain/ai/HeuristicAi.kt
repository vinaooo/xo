package io.github.vinaooo.xo.domain.ai

import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.Lines
import io.github.vinaooo.xo.domain.model.Mark
import kotlin.random.Random

/**
 * Medium: wins when it can, blocks the player's winning cell, otherwise takes a center cell, otherwise any. It
 * doesn't see forks, so it can be beaten.
 */
object HeuristicAi : Ai {
    override fun move(state: GameState, random: Random): Int {
        val board = state.board
        val me = state.toMove
        winningCells(board, me).randomOrNull(random)?.let { return it }
        winningCells(board, me.other).randomOrNull(random)?.let { return it }
        return centerCells(board).filter { board[it] == null }.randomOrNull(random) ?: board.emptyCells.random(random)
    }
}

/** The empty cells that would complete a line of [mark]. */
internal fun winningCells(board: Board, mark: Mark): List<Int> = Lines.of(board.size)
    .filter { line -> line.count { board[it] == mark } == line.size - 1 && line.count { board[it] == null } == 1 }
    .map { line -> line.first { board[it] == null } }
    .distinct()

/** The middle cell of an odd board, the middle four of an even one. */
internal fun centerCells(board: Board): List<Int> {
    val side = board.size.side
    val middle = if (side % 2 == 1) listOf(side / 2) else listOf(side / 2 - 1, side / 2)
    return middle.flatMap { row -> middle.map { column -> row * side + column } }
}
