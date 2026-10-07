package io.github.vinaooo.xo.data

import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileSavedGameRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val dispatcher = StandardTestDispatcher()
    private val file get() = File(dir, "saved_game.json")
    private val repository get() = FileSavedGameRepository(file, dispatcher)
    private val engine = GameEngine()

    private val session = GameSession(11, engine.newGame(GameMode(BoardSize.FIVE, Opponent.MEDIUM), Mark.O))
        .play(Move.Place(12), engine).shouldNotBeNull()
        .play(Move.Place(0), engine).shouldNotBeNull()
        .play(Move.Place(6), engine).shouldNotBeNull()

    @Test
    fun `nothing saved loads as null`() = runTest(dispatcher) {
        repository.load().shouldBeNull()
    }

    @Test
    fun `a saved session loads back with its undo and redo`() = runTest(dispatcher) {
        val undone = session.undo().shouldNotBeNull()
        repository.save(undone)

        val loaded = FileSavedGameRepository(file, dispatcher).load()

        loaded shouldBe undone
        loaded!!.redo()!!.state shouldBe session.state
    }

    @Test
    fun `saving again replaces the previous game`() = runTest(dispatcher) {
        repository.save(session)
        val newer = session.play(Move.Place(24), engine).shouldNotBeNull()
        repository.save(newer)

        repository.load() shouldBe newer
    }

    @Test
    fun `clear removes the saved game`() = runTest(dispatcher) {
        repository.save(session)
        repository.clear()

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a corrupted file is discarded instead of crashing`() = runTest(dispatcher) {
        file.writeText("{ not json")

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a board of the wrong size is discarded`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replaceFirst("\"size\":\"FIVE\"", "\"size\":\"THREE\""))

        repository.load().shouldBeNull()
        file.exists().shouldBeFalse()
    }

    @Test
    fun `a file from an unknown future version is ignored`() = runTest(dispatcher) {
        repository.save(session)
        file.writeText(file.readText().replace("\"version\":1", "\"version\":99"))

        repository.load().shouldBeNull()
    }

    @Test
    fun `no temporary files are left behind`() = runTest(dispatcher) {
        repository.save(session)

        dir.listFiles()!!.map { it.name } shouldBe listOf("saved_game.json")
    }
}
