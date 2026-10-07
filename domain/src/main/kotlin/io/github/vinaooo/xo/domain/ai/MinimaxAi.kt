package io.github.vinaooo.xo.domain.ai

import io.github.vinaooo.xo.domain.model.Board
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameState
import io.github.vinaooo.xo.domain.model.Lines
import io.github.vinaooo.xo.domain.model.Mark
import kotlin.random.Random

/**
 * Hard: negamax with alpha-beta. On 3×3 it searches to the end, so it never loses. On 4×4 and 5×5 a full search is
 * too slow for a phone, so it looks [depthFor] moves ahead and scores the board by its open lines: very strong, not
 * provably perfect. Among equally good moves it picks at random, so games vary.
 */
object MinimaxAi : Ai {
    override fun move(state: GameState, random: Random): Int =
        obviousMove(state.board, state.toMove, random) ?: searchedMove(state.board, state.toMove, random)

    /**
     * A win now, or the only block, needs no search; nor does the opening on the bigger boards, where the widest
     * search (~650 ms on a phone) would only pick a center cell.
     */
    internal fun obviousMove(board: Board, me: Mark, random: Random): Int? {
        winningCells(board, me).firstOrNull()?.let { return it }
        val threats = winningCells(board, me.other)
        return when {
            threats.size == 1 -> threats.single()
            board.size != BoardSize.THREE && board.cells.count { it != null } <= OPENING_MARKS ->
                centerCells(board).filter { board[it] == null }.randomOrNull(random)
            else -> null
        }
    }

    private fun searchedMove(board: Board, me: Mark, random: Random): Int {
        val search = Search(board.size, depthFor(board))
        val scored = orderedMoves(board, me).map { cell ->
            cell to -search.negamax(board.place(cell, me), me.other, 1, -WIN, WIN)
        }
        val best = scored.maxOf { it.second }
        // Perfect play draws 4×4 and 5×5, so many moves tie; take the one leaving the most open lines, which is what
        // beats a player who errs.
        val safe = scored.filter { it.second == best }.map { it.first }
        val potential = safe.associateWith { search.potential(board.place(it, me), me) }
        val top = potential.values.max()
        return safe.filter { potential.getValue(it) == top }.random(random)
    }

    /**
     * How many positions hard visits to answer [state]: its cost, the same on every machine (a time limit isn't: CI's
     * runners are slower than a phone). 0 for an obvious move.
     */
    internal fun searchCost(state: GameState): Int {
        val board = state.board
        val me = state.toMove
        if (obviousMove(board, me, Random(0)) != null) return 0
        val search = Search(board.size, depthFor(board))
        orderedMoves(board, me).forEach { search.negamax(board.place(it, me), me.other, 1, -WIN, WIN) }
        return search.visited
    }

    /** [board]'s value for [toMove] as the search sees it: a win sooner is worth more, a draw 0. */
    internal fun value(board: Board, toMove: Mark): Int =
        Search(board.size, depthFor(board)).negamax(board, toMove, 0, -WIN, WIN)

    /** [board]'s open lines for [mark], against its opponent's. */
    internal fun potential(board: Board, mark: Mark): Int = Search(board.size, depthFor(board)).potential(board, mark)

    /** How many moves ahead: all of them on 3×3; on bigger boards, more as the board fills up. */
    internal fun depthFor(board: Board): Int = when (board.size) {
        BoardSize.THREE -> board.size.cells
        BoardSize.FOUR -> if (board.emptyCells.size <= DEEP_ENDGAME) board.size.cells else FOUR_DEPTH
        BoardSize.FIVE -> if (board.emptyCells.size <= DEEP_ENDGAME) FOUR_DEPTH else FIVE_DEPTH
    }

    /** One search, with its transposition table. */
    private class Search(private val size: BoardSize, private val maxDepth: Int) {
        private val seen = HashMap<String, Int>()

        /** Positions looked at so far. */
        var visited = 0
            private set

        /** The value of [board] for [toMove], [depth] moves below the root. Faster wins score higher. */
        fun negamax(board: Board, toMove: Mark, depth: Int, alphaIn: Int, beta: Int): Int {
            visited++
            leafValue(board, toMove, depth)?.let { return it }
            val key = "${board.cells.joinToString("") { it?.name ?: "." }}$toMove$depth"
            seen[key]?.let { return it }
            var alpha = alphaIn
            var best = -WIN
            for (cell in orderedMoves(board, toMove)) {
                val value = -negamax(board.place(cell, toMove), toMove.other, depth + 1, -beta, -alpha)
                if (value > best) best = value
                if (best > alpha) alpha = best
                if (alpha >= beta) break
            }
            // Only exact values are reused; a cut-off one is only a bound.
            if (best in (alphaIn + 1) until beta) seen[key] = best
            return best
        }

        /** The value where the search stops: a loss (sooner is worse), a draw, or the horizon's estimate. */
        private fun leafValue(board: Board, toMove: Mark, depth: Int): Int? = when {
            Lines.of(size).any { line -> line.all { board[it] == toMove.other } } -> -(WIN - depth)
            board.isFull -> 0
            depth >= maxDepth -> evaluate(board, toMove)
            else -> null
        }

        /** [board]'s open lines for [mark], against its opponent's. */
        fun potential(board: Board, mark: Mark): Int = evaluate(board, mark)

        /** Open lines: each counts by how full it is, for [toMove] or against. */
        private fun evaluate(board: Board, toMove: Mark): Int = Lines.of(size).sumOf { line ->
            val mine = line.count { board[it] == toMove }
            val theirs = line.count { board[it] == toMove.other }
            when {
                mine > 0 && theirs > 0 -> 0
                mine > 0 -> LINE_WEIGHTS[mine]
                theirs > 0 -> -LINE_WEIGHTS[theirs]
                else -> 0
            }
        }
    }

    internal const val WIN = 1_000_000
    private const val OPENING_MARKS = 1
    private const val FOUR_DEPTH = 6
    private const val FIVE_DEPTH = 4
    private const val DEEP_ENDGAME = 9

    /** A line with 1, 2, 3 marks and no opposing ones; index = marks. */
    private val LINE_WEIGHTS = intArrayOf(0, 1, 10, 100, 1_000)
}

/** Winning cells first, then blocks, then the cells on the most lines still open to [toMove]: alpha-beta cuts more. */
internal fun orderedMoves(board: Board, toMove: Mark): List<Int> {
    val wins = winningCells(board, toMove).toSet()
    val blocks = winningCells(board, toMove.other).toSet()
    return board.emptyCells.sortedByDescending { cell ->
        when (cell) {
            in wins -> ORDER_WIN
            in blocks -> ORDER_BLOCK
            else -> Lines.through(board.size, cell).count { line -> line.none { board[it] == toMove.other } }
        }
    }
}

private const val ORDER_WIN = 1_000
private const val ORDER_BLOCK = 500
