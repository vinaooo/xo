package io.github.vinaooo.xo.feature.game

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.feature.game.ui.GameScreen
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-port")
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()
    private val intents = mutableListOf<GameIntent>()

    private fun session(mode: GameMode, vararg cells: Int) = cells.fold(GameSession(1, engine.newGame(mode, Mark.X))) {
            s,
            cell,
        ->
        s.play(Move.Place(cell), engine)!!
    }

    private fun show(state: GameUiState) = compose.setContent {
        VinkitTheme(ThemeColor.PURPLE) { GameScreen(state, { intents += it }) }
    }

    @Test
    fun `the info names the mode and whose turn it is`() {
        show(GameUiState(session = session(GameMode(BoardSize.FOUR, Opponent.HARD))))
        compose.onNodeWithContentDescription("4×4 · Hard, Your turn").assertExists()
    }

    @Test
    fun `every cell is a TalkBack node, and an empty one plays when activated`() {
        show(GameUiState(session = session(GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER), 12)))
        compose.onAllNodesWithContentDescription("row", substring = true).assertCountEquals(25)
        compose.onNodeWithContentDescription("row 3, column 3, X").assertExists()
        compose.onNodeWithContentDescription("row 1, column 2, empty").performSemanticsAction(SemanticsActions.OnClick)
        intents shouldBe listOf(GameIntent.Place(1))
    }

    @Test
    fun `while the AI thinks, cells can't be played and the info says so`() {
        show(GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.MEDIUM), 4), aiThinking = true))
        compose.onNodeWithContentDescription("3×3 · Medium, Thinking…").assertExists()
        compose.onNodeWithContentDescription("row 1, column 1, empty")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `the toolbar sends undo and hint, and a shown hint is marked on its cell`() {
        show(GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.TWO_PLAYER), 4, 0), hint = 8))
        compose.onNodeWithContentDescription("row 3, column 3, empty, suggested").assertExists()
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.onNodeWithContentDescription("Play the hint").performClick()
        intents shouldBe listOf(GameIntent.Undo, GameIntent.Hint)
    }

    @Test
    fun `a won game shows its result after the winning line, with a new game`() {
        show(GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.TWO_PLAYER), 0, 3, 1, 4, 2)))
        compose.onNodeWithContentDescription("3×3 · 2 players, X wins!").assertExists()
        compose.mainClock.advanceTimeBy(END_DIALOG_MILLIS)
        compose.onNodeWithText("Moves: 5").assertExists()
        compose.onNodeWithText("New game").performClick()
        intents shouldBe listOf(GameIntent.NewGame)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun `landscape keeps the board and the toolbar`() {
        show(GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.EASY))))
        compose.onAllNodesWithContentDescription("row", substring = true).assertCountEquals(9)
        compose.onNodeWithContentDescription("Hint").performClick()
        intents shouldBe listOf(GameIntent.Hint)
    }

    private companion object {
        const val END_DIALOG_MILLIS = 2_000L
    }
}
