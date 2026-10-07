package io.github.vinaooo.xo.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.vinaooo.xo.MainActivity
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.repository.SavedGameRepository
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Debug builds only. Saves a test game and opens it, to try a position without playing up to it. Start it with `-S`,
 * so a running game screen can't overwrite the save:
 * `adb shell am start -S -n io.github.vinaooo.xo/.debug.DebugGameActivity --es game near_win`
 * - `near_win` (the default): 3×3 against hard, the player one move from a win (top row).
 * - `near_loss`: 3×3 against hard, the AI one move from a win the player must block.
 * - `five_full`: 5×5 2-player, one empty cell left, which ends it in a draw.
 *
 * Or replay a bug report:
 * - `--es state <code>`: the code in the report's "State:" block (GitHub or email), the exact board.
 * - `--es load game.json`: the report's attached game, with its moves to undo, pushed first to the app's folder:
 *   `adb push game.json /sdcard/Android/data/io.github.vinaooo.xo/files/`
 */
@AndroidEntryPoint
class DebugGameActivity : ComponentActivity() {

    @Inject lateinit var savedGames: SavedGameRepository

    @Inject lateinit var engine: GameEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlocking {
            val report = intent.getStringExtra("state")?.let { GameSession(SEED, GameSession.codec.decode(it)) }
                ?: intent.getStringExtra("load")?.let {
                    Json.decodeFromString(GameSession.serializer(), File(getExternalFilesDir(null), it).readText())
                }
            savedGames.save(report ?: preset(intent.getStringExtra("game")))
        }
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK,
            ),
        )
        finish()
    }

    private fun preset(name: String?): GameSession = when (name) {
        // X . . / O O . / X . . with X (the player) to move: the AI threatens 5.
        "near_loss" -> play(GameMode(BoardSize.THREE, Opponent.HARD), 0, 3, 6, 4)
        "five_full" -> play(GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER), *FIVE_ALMOST_FULL)
        // X X . / O O . / . . . with X (the player) to move: 2 wins.
        else -> play(GameMode(BoardSize.THREE, Opponent.HARD), 0, 3, 1, 4)
    }

    private fun play(mode: GameMode, vararg cells: Int): GameSession =
        cells.fold(GameSession(SEED, engine.newGame(mode, Mark.X))) { session, cell ->
            checkNotNull(session.play(Move.Place(cell), engine)) { "Preset move $cell is illegal" }
        }

    private companion object {
        const val SEED = 7L

        /** XXOOX / OOXXO rows: 24 of 25 cells, no line of four for anyone; X's last move in cell 24 draws. */
        val FIVE_ALMOST_FULL = intArrayOf(
            0, 2, 1, 3, 4, 5, 7, 6, 8, 9, 10, 12, 11, 13, 14, 15, 17, 16, 18, 19, 20, 22, 21, 23,
        )
    }
}
