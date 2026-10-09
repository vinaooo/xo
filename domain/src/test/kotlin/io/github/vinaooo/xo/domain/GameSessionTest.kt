package io.github.vinaooo.xo.domain

import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class GameSessionTest {
    private val engine = GameEngine()

    private fun session(opponent: Opponent, first: Mark = Mark.X) =
        GameSession(seed = 7, state = engine.newGame(GameMode(BoardSize.THREE, opponent), first))

    private fun GameSession.place(vararg cells: Int) = cells.fold(this) { s, cell ->
        s.play(Move.Place(cell), engine)!!
    }

    @Test
    fun `in 2-player, undo and redo step one move at a time`() {
        val played = session(Opponent.TWO_PLAYER).place(4, 0)
        val undone = played.undo()!!
        undone.state.board[0].shouldBeNull()
        undone.state.board[4] shouldBe Mark.X
        undone.state.toMove shouldBe Mark.O
        undone.redo()!!.state shouldBe played.state
    }

    @Test
    fun `against the AI, undo takes back the AI's reply with the player's move, and redo replays both`() {
        val played = session(Opponent.HARD).place(4, 0, 8, 2)
        val undone = played.undo()!!
        undone.state shouldBe session(Opponent.HARD).place(4, 0).state
        undone.undo()!!.state shouldBe session(Opponent.HARD).state
        undone.redo()!!.state shouldBe played.state
    }

    @Test
    fun `when the AI opened, nothing before the player's first move can be undone`() {
        val aiOpened = session(Opponent.HARD, first = Mark.O).place(4)
        aiOpened.canUndo shouldBe false
        aiOpened.undo().shouldBeNull()
        val afterReply = aiOpened.place(0, 8)
        afterReply.undo()!!.state shouldBe aiOpened.state
    }

    @Test
    fun `a new move clears redo, and a finished game can't be undone or redone`() {
        val played = session(Opponent.TWO_PLAYER).place(4, 0)
        played.undo()!!.place(1).canRedo shouldBe false
        val won = session(Opponent.TWO_PLAYER).place(0, 3, 1, 4, 2)
        won.canUndo shouldBe false
        won.undo().shouldBeNull()
        won.undo()?.redo().shouldBeNull()
    }

    @Test
    fun `undos are counted, and redo doesn't take one back`() {
        val played = session(Opponent.HARD).place(0, 4)
        played.undosUsed shouldBe 0
        played.undo()!!.undosUsed shouldBe 1
        played.undo()!!.redo()!!.undosUsed shouldBe 1
    }

    @Test
    fun `in progress means moves made and not over`() {
        session(Opponent.EASY).isInProgress shouldBe false
        session(Opponent.EASY).place(4).isInProgress shouldBe true
        session(Opponent.TWO_PLAYER).place(0, 3, 1, 4, 2).isInProgress shouldBe false
    }

    @Test
    fun `an illegal move leaves the session unplayed`() {
        session(Opponent.EASY).place(4).play(Move.Place(4), engine).shouldBeNull()
    }

    @Test
    fun `a session survives JSON, and its state survives the bug report codec`() {
        val played = session(Opponent.MEDIUM).place(4, 0, 8).undo()!!
        Json.decodeFromString(GameSession.serializer(), Json.encodeToString(GameSession.serializer(), played)) shouldBe
            played
        val won = session(Opponent.TWO_PLAYER).place(0, 3, 1, 4, 2)
        GameSession.codec.decode(GameSession.codec.encode(won.state)) shouldBe won.state
    }

    @Test
    fun `modes have stable keys and come back from them`() {
        GameMode(BoardSize.FIVE, Opponent.HARD).key shouldBe "FIVE_HARD"
        GameMode.ALL.size shouldBe 12
        GameMode.ALL.forEach { GameMode.fromKey(it.key) shouldBe it }
        GameMode.fromKey("SEVEN_EASY").shouldBeNull()
    }
}
