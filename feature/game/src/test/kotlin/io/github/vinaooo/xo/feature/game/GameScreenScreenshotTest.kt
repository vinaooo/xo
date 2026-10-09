package io.github.vinaooo.xo.feature.game

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.Handedness
import io.github.vinaooo.vinkit.core.PhoneViewSide
import io.github.vinaooo.vinkit.core.ThemeColor
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.designsystem.VinkitTheme
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.feature.game.ui.GameScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The game screen as players see it, in the layouts and themes it supports. Dynamic color off: the brand purple. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-port-xxhdpi")
class GameScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val engine = GameEngine()

    private fun session(mode: GameMode, vararg cells: Int) = cells.fold(GameSession(1, engine.newGame(mode, Mark.X))) {
            s,
            cell,
        ->
        s.play(Move.Place(cell), engine)!!
    }

    private fun shoot(name: String, state: GameUiState, settings: AppSettings = AppSettings()) {
        val app = settings.copy(dynamicColor = false, themeColor = ThemeColor.PURPLE)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            VinkitTheme(app.themeColor, app.themeMode, app.dynamicColor) {
                GameScreen(state.copy(settings = app), {}, onOpenScores = {}, onOpenSettings = {}, onOpenBadges = {})
            }
        }
        // Marks drawn in, the winning line struck, the end dialog not yet shown.
        compose.mainClock.advanceTimeBy(SETTLE_MILLIS)
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun midGame3x3Light() = shoot(
        "game_3x3_light",
        GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.MEDIUM), 4, 0, 8, 2)),
    )

    @Test
    fun hint4x4Dark() = shoot(
        "game_4x4_dark_hint",
        GameUiState(session = session(GameMode(BoardSize.FOUR, Opponent.HARD), 5, 6, 9), hint = 10),
        AppSettings(themeMode = ThemeMode.DARK),
    )

    @Test
    fun won5x5() = shoot(
        "game_5x5_won",
        GameUiState(session = session(GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER), 6, 0, 12, 1, 18, 2, 24)),
    )

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun landscapeLeftHand() = shoot(
        "game_landscape_left_hand",
        GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.EASY), 4, 0)),
        AppSettings(handedness = Handedness.LEFT),
    )

    @Test
    @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-mdpi")
    fun tabletPhoneView() = shoot(
        "game_tablet_phone_view",
        GameUiState(session = session(GameMode(BoardSize.FOUR, Opponent.MEDIUM), 5, 10)),
        AppSettings(phoneView = true, phoneViewSide = PhoneViewSide.RIGHT),
    )

    @Test
    @Config(qualifiers = "pt-rBR-w411dp-h891dp-port-xxhdpi")
    fun portuguese() = shoot(
        "game_pt_br",
        GameUiState(session = session(GameMode(BoardSize.THREE, Opponent.TWO_PLAYER), 4)),
    )

    private companion object {
        const val SETTLE_MILLIS = 600L
    }
}
