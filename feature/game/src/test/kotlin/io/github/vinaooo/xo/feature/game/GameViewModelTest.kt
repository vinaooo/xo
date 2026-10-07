package io.github.vinaooo.xo.feature.game

import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.xo.domain.fake.FakeGameSettingsRepository
import io.github.vinaooo.xo.domain.fake.FakeSavedGameRepository
import io.github.vinaooo.xo.domain.fake.FakeSeedSource
import io.github.vinaooo.xo.domain.fake.FakeStatsRepository
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.repository.GameSettings
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.domain.usecase.FinishGame
import io.github.vinaooo.xo.domain.usecase.ResumeGame
import io.github.vinaooo.xo.domain.usecase.SaveGame
import io.github.vinaooo.xo.domain.usecase.StartNewGame
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val engine = GameEngine()
    private val savedGames = FakeSavedGameRepository()
    private val gameSettings = FakeGameSettingsRepository(GameSettings(mode = GameMode(BoardSize.THREE, Opponent.HARD)))
    private val stats = FakeStatsRepository()
    private val appSettings = object : AppSettingsRepository {
        val current = MutableStateFlow(AppSettings())
        override val settings = current

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            current.value = transform(current.value)
        }
    }
    private val played = mutableListOf<String>()
    private val feedback = object : GameFeedback {
        override fun sound(event: FeedbackEvent) {
            played += "sound $event"
        }

        override fun haptic(event: FeedbackEvent) = Unit
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): GameViewModel = GameViewModel(
        StartNewGame(savedGames, gameSettings, stats, FakeSeedSource(), engine),
        ResumeGame(savedGames),
        SaveGame(savedGames),
        FinishGame(savedGames, stats),
        appSettings,
        gameSettings,
        engine,
        feedback,
        dispatcher,
    ).also { advanceUntilIdle() }

    private fun GameViewModel.session() = uiState.value.session.shouldNotBeNull()

    @Test
    fun `the first launch starts a game in Settings' mode, the player opening`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.session().state.mode shouldBe GameMode(BoardSize.THREE, Opponent.HARD)
        vm.session().state.firstMover shouldBe Mark.X
        vm.uiState.value.canPlay shouldBe true
    }

    @Test
    fun `a saved game is resumed`() = runTest(dispatcher) {
        val saved = GameSession(5, engine.newGame(GameMode(BoardSize.FIVE, Opponent.EASY), Mark.X))
        savedGames.saved = saved
        viewModel().session() shouldBe saved
    }

    @Test
    fun `the AI answers the player's move after a pause, and both are saved and announced`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.Place(4))
        vm.uiState.value.aiThinking shouldBe true
        vm.uiState.value.canPlay shouldBe false
        advanceUntilIdle()
        val state = vm.session().state
        state.moves shouldBe 2
        state.board[4] shouldBe Mark.X
        state.toMove shouldBe Mark.X
        vm.uiState.value.aiThinking shouldBe false
        savedGames.saved shouldBe vm.session()
        vm.uiState.value.announcement.shouldBeInstanceOf<Announcement.Placed>().mark shouldBe Mark.O
        played shouldBe listOf("sound MOVE", "sound MOVE")
    }

    @Test
    fun `when the AI opens a new game, it plays first`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.NewGame)
        advanceUntilIdle()
        vm.session().state.firstMover shouldBe Mark.O
        vm.session().state.moves shouldBe 1
    }

    @Test
    fun `a taken cell is refused with its sound, and taps wait for the AI`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.Place(4))
        vm.onIntent(GameIntent.Place(0))
        advanceUntilIdle()
        vm.session().state.moves shouldBe 2
        vm.onIntent(GameIntent.Place(4))
        played.last() shouldBe "sound REJECTED"
    }

    @Test
    fun `undo takes back the AI's answer with the player's move, even while it is still thinking`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(GameIntent.Place(4))
            advanceUntilIdle()
            vm.onIntent(GameIntent.Place(vm.session().state.board.emptyCells.first()))
            vm.onIntent(GameIntent.Undo)
            advanceUntilIdle()
            vm.session().state.moves shouldBe 2
            vm.uiState.value.announcement shouldBe Announcement.Undone
            vm.onIntent(GameIntent.Undo)
            vm.session().state.moves shouldBe 0
            vm.onIntent(GameIntent.Redo)
            vm.session().state.moves shouldBe 2
        }

    @Test
    fun `the hint shows the best cell, and a second tap plays it`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onIntent(GameIntent.Hint)
        advanceUntilIdle()
        val hint = vm.uiState.value.hint.shouldNotBeNull()
        vm.uiState.value.announcement.shouldBeInstanceOf<Announcement.Hinted>()
        vm.onIntent(GameIntent.Hint)
        vm.session().state.board[hint] shouldBe Mark.X
        vm.uiState.value.hint.shouldBeNull()
    }

    @Test
    fun `a 2-player win is celebrated, recorded nowhere, and clears the save`() = runTest(dispatcher) {
        gameSettings.current.value = GameSettings(mode = GameMode(BoardSize.THREE, Opponent.TWO_PLAYER))
        val vm = viewModel()
        listOf(0, 3, 1, 4, 2).forEach { vm.onIntent(GameIntent.Place(it)) }
        advanceUntilIdle()
        vm.session().state.status shouldBe GameStatus.Won(Mark.X, listOf(0, 1, 2))
        played.last() shouldBe "sound WIN"
        vm.uiState.value.announcement shouldBe Announcement.Ended(GameStatus.Won(Mark.X, listOf(0, 1, 2)), false)
        stats.stats.value shouldBe emptyMap()
        savedGames.saved.shouldBeNull()
    }

    @Test
    fun `a game against the AI that ends goes into the stats`() = runTest(dispatcher) {
        val vm = viewModel()
        while (!vm.session().state.isOver) {
            vm.onIntent(GameIntent.Place(vm.session().state.board.emptyCells.first()))
            advanceUntilIdle()
        }
        // The hard AI never loses on 3×3.
        val key = GameMode(BoardSize.THREE, Opponent.HARD).key
        stats.stats.value.getValue(key).let { (it.played to it.won) } shouldBe (1 to 0)
    }

    @Test
    fun `a mode changed in Settings starts a game in it`() = runTest(dispatcher) {
        val vm = viewModel()
        val five = GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER)
        gameSettings.update { it.copy(mode = five) }
        advanceUntilIdle()
        vm.session().state.mode shouldBe five
    }

    @Test
    fun `sound follows the player's settings`() = runTest(dispatcher) {
        appSettings.current.value = AppSettings(soundEnabled = false)
        val vm = viewModel()
        vm.onIntent(GameIntent.Place(4))
        advanceUntilIdle()
        played shouldBe emptyList()
        stats.stats.value shouldBe emptyMap<String, GameStats>()
    }
}
