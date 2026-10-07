package io.github.vinaooo.xo.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.feature.game.settings.GameSection
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the game section picks the board and the opponent, and explains the opponent`() {
        val changes = mutableListOf<GameMode>()
        compose.setContent {
            // vinkit's section card lays its rows out in a column; so does this test.
            VinkitTheme(ThemeColor.PURPLE) {
                Column { GameSection(GameMode(BoardSize.THREE, Opponent.HARD)) { changes += it } }
            }
        }
        compose.onNodeWithText("Thinks ahead: never loses on 3×3").assertExists()
        compose.onNodeWithText("5×5").performClick()
        compose.onNodeWithContentDescription("2 players").performClick()
        changes shouldContainExactly listOf(
            GameMode(BoardSize.FIVE, Opponent.HARD),
            GameMode(BoardSize.THREE, Opponent.TWO_PLAYER),
        )
    }
}
