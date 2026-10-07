package io.github.vinaooo.xo.feature.game

import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class BugReportTest {
    private val engine = GameEngine()

    @Test
    fun `a report carries the settings, the game, its replayable state and its session file`() {
        val session = GameSession(77, engine.newGame(GameMode(BoardSize.FOUR, Opponent.HARD), Mark.O))
            .play(Move.Place(5), engine)!!
        val report = gameReport(GameUiState(session = session))

        report.details shouldHaveSize 2
        report.details[0] shouldContain "Settings: theme SYSTEM"
        report.details[1] shouldContain "Game: seed 77, FOUR HARD, opened by O, 1 moves"
        GameSession.codec.decode(report.state!!) shouldBe session.state
        Json.decodeFromString(GameSession.serializer(), report.files.getValue("game.json")) shouldBe session
    }

    @Test
    fun `before a game loads, the report has the settings alone`() {
        val report = gameReport(GameUiState())
        report.details shouldHaveSize 1
        report.state shouldBe null
    }

    @Test
    fun `reports go to OX Play's own address and repository`() {
        REPORT_TARGET.email shouldBe "vrpedrinho+xo@gmail.com"
        REPORT_TARGET.issuesUrl shouldBe "https://github.com/vinaooo/xo/issues/new"
    }
}
