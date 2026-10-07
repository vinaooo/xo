package io.github.vinaooo.xo.feature.game

import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.xo.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.xo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.domain.usecase.ResumeGame
import io.github.vinaooo.xo.feature.game.settings.PendingMode
import io.github.vinaooo.xo.feature.game.settings.SettingsViewModel
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val engine = GameEngine()
    private val savedGames = FakeSavedGameRepository()
    private val gameSettings = FakeGameSettingsRepository()
    private val appSettings = object : AppSettingsRepository {
        val current = MutableStateFlow(AppSettings())
        override val settings = current

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            current.value = transform(current.value)
        }
    }
    private val five = GameMode(BoardSize.FIVE, Opponent.HARD)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(appSettings, gameSettings, ResumeGame(savedGames))

    private fun inProgress(opponent: Opponent) =
        GameSession(1, engine.newGame(GameMode(BoardSize.THREE, opponent), Mark.X))
            .play(Move.Place(4), engine)

    @Test
    fun `with no game on, a new mode applies at once`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onModeChange(five)
        advanceUntilIdle()
        gameSettings.current.value.mode shouldBe five
        vm.pendingMode.value.shouldBeNull()
    }

    @Test
    fun `with an AI game on, it asks first, saying the game counts as a loss`() = runTest(dispatcher) {
        savedGames.saved = inProgress(Opponent.EASY)
        val vm = viewModel()
        vm.onModeChange(five)
        advanceUntilIdle()
        vm.pendingMode.value shouldBe PendingMode(five, countsAsLoss = true)
        gameSettings.current.value.mode shouldBe GameMode.DEFAULT
        vm.confirmMode()
        advanceUntilIdle()
        gameSettings.current.value.mode shouldBe five
        vm.pendingMode.value.shouldBeNull()
    }

    @Test
    fun `a 2-player game on also asks, without the loss, and dismissing keeps the mode`() = runTest(dispatcher) {
        savedGames.saved = inProgress(Opponent.TWO_PLAYER)
        val vm = viewModel()
        vm.onModeChange(five)
        advanceUntilIdle()
        vm.pendingMode.value shouldBe PendingMode(five, countsAsLoss = false)
        vm.dismissMode()
        advanceUntilIdle()
        vm.pendingMode.value.shouldBeNull()
        gameSettings.current.value.mode shouldBe GameMode.DEFAULT
    }

    @Test
    fun `appearance changes go to vinkit's settings`() = runTest(dispatcher) {
        viewModel().onAppChange { it.copy(themeMode = ThemeMode.DARK) }
        advanceUntilIdle()
        appSettings.current.value.themeMode shouldBe ThemeMode.DARK
    }
}
