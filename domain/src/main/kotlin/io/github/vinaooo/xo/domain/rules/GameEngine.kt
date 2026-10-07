package io.github.vinaooo.xo.domain.rules

import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move

sealed interface MoveOutcome {
    data class Applied(val state: GameState) : MoveOutcome

    data object Rejected : MoveOutcome
}

/** The rules: marks go on empty cells in turn; a full line wins, a full board without one is a draw. */
class GameEngine {
    fun newGame(mode: GameMode, firstMover: Mark): GameState =
        GameState(Board.empty(mode.size), mode, toMove = firstMover, firstMover = firstMover)

    fun isLegal(state: GameState, move: Move): Boolean = when (move) {
        is Move.Place -> !state.isOver && move.cell in state.board.cells.indices && state.board[move.cell] == null
    }

    fun legalMoves(state: GameState): List<Move> =
        if (state.isOver) emptyList() else state.board.emptyCells.map(Move::Place)

    fun apply(state: GameState, move: Move): MoveOutcome {
        if (!isLegal(state, move)) return MoveOutcome.Rejected
        return when (move) {
            is Move.Place -> {
                val board = state.board.place(move.cell, state.toMove)
                val line = board.winningLineThrough(move.cell)
                val status = when {
                    line != null -> GameStatus.Won(state.toMove, line)
                    board.isFull -> GameStatus.Draw
                    else -> GameStatus.Playing
                }
                MoveOutcome.Applied(
                    state.copy(board = board, toMove = state.toMove.other, status = status, moves = state.moves + 1),
                )
            }
        }
    }
}
