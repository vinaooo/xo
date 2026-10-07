package io.github.vinaooo.xo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.vinkit.core.AppSettingsRepository
import io.github.vinaooo.vinkit.shell.FeedbackEvent
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.vinkit.shell.give
import io.github.vinaooo.xo.domain.ai.MinimaxAi
import io.github.vinaooo.xo.domain.ai.aiFor
import io.github.vinaooo.xo.domain.ai.random
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.model.Move
import io.github.vinaooo.xo.domain.repository.GameSettingsRepository
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import io.github.vinaooo.xo.domain.usecase.FinishGame
import io.github.vinaooo.xo.domain.usecase.ResumeGame
import io.github.vinaooo.xo.domain.usecase.SaveGame
import io.github.vinaooo.xo.domain.usecase.StartNewGame
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList") // Each collaborator is one small, separately tested responsibility.
@HiltViewModel
class GameViewModel @Inject constructor(
    private val startNewGame: StartNewGame,
    private val resumeGame: ResumeGame,
    private val saveGame: SaveGame,
    private val finishGame: FinishGame,
    appSettings: AppSettingsRepository,
    gameSettings: GameSettingsRepository,
    private val engine: GameEngine,
    private val feedback: GameFeedback,
    @AiDispatcher private val aiDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val state = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = state.asStateFlow()

    /** The AI's pending answer, or the hint being worked out: undo and new game cancel it. */
    private var thinking: Job? = null

    init {
        viewModelScope.launch {
            appSettings.settings.collect { settings -> state.update { it.copy(settings = settings) } }
        }
        viewModelScope.launch { show(resumeGame() ?: startNewGame()) }
        viewModelScope.launch {
            // A mode changed in Settings (confirmed there when a game was on) starts a game in it.
            gameSettings.settings.map { it.mode }.distinctUntilChanged().drop(1).collect { newGame(it) }
        }
    }

    fun onIntent(intent: GameIntent) {
        when (intent) {
            is GameIntent.Place -> place(intent.cell)
            GameIntent.Undo -> step { it.undo() }?.let { announce(Announcement.Undone) }
            GameIntent.Redo -> step { it.redo() }?.let { announce(Announcement.Redone) }
            GameIntent.Hint -> hint()
            GameIntent.NewGame -> viewModelScope.launch { newGame(null) }
        }
    }

    private suspend fun newGame(mode: GameMode?) {
        thinking?.cancel()
        show(startNewGame(mode))
    }

    private fun show(session: GameSession) {
        state.update { it.copy(session = session, hint = null, aiThinking = false) }
        if (session.state.isAiTurn) answer(session)
    }

    private fun place(cell: Int) {
        val current = state.value
        val session = current.session?.takeIf { current.canPlay } ?: return
        val played = session.play(Move.Place(cell), engine)
        if (played == null) {
            feedback.give(FeedbackEvent.REJECTED, current.settings)
            return
        }
        moved(played, cell)
    }

    /** After any placement, the player's or the AI's: save, sound, and then the end or the AI's answer. */
    private fun moved(session: GameSession, cell: Int) {
        val placed = state.value.session?.state?.toMove ?: return
        state.update { it.copy(session = session, hint = null, aiThinking = false) }
        announce(Announcement.Placed(placed, cell, session.state.board.size.side))
        feedback.give(FeedbackEvent.MOVE, state.value.settings)
        viewModelScope.launch {
            if (session.state.isOver) finish(session) else saveGame(session)
        }
        if (session.state.isAiTurn) answer(session)
    }

    private suspend fun finish(session: GameSession) {
        val status = session.state.status
        val playerWon = status is GameStatus.Won && (!session.state.mode.isVsAi || status.mark == GameMode.HUMAN)
        if (playerWon) feedback.give(FeedbackEvent.WIN, state.value.settings)
        announce(Announcement.Ended(status, session.state.mode.isVsAi))
        finishGame(session)
    }

    /** The AI answers after a short pause, so its move is seen arriving. */
    private fun answer(session: GameSession) {
        val ai = aiFor(session.state.mode.opponent) ?: return
        state.update { it.copy(aiThinking = true) }
        thinking = viewModelScope.launch {
            delay(AI_PAUSE_MILLIS)
            val cell = withContext(aiDispatcher) { ai.move(session.state, session.random()) }
            session.play(Move.Place(cell), engine)?.let { moved(it, cell) }
        }
    }

    /** First tap: the best move for the side to move shows. Second tap: it's played. */
    private fun hint() {
        val current = state.value
        val session = current.session?.takeIf { current.canPlay } ?: return
        current.hint?.let {
            place(it)
            return
        }
        thinking = viewModelScope.launch {
            val cell = withContext(aiDispatcher) { MinimaxAi.move(session.state, session.random()) }
            state.update { it.copy(hint = cell) }
            announce(Announcement.Hinted(cell, session.state.board.size.side))
        }
    }

    /** Undo or redo: any pending AI answer or hint is dropped. */
    private fun step(move: (GameSession) -> GameSession?): GameSession? {
        val stepped = state.value.session?.let(move) ?: return null
        thinking?.cancel()
        state.update { it.copy(session = stepped, hint = null, aiThinking = false) }
        viewModelScope.launch { saveGame(stepped) }
        return stepped
    }

    private fun announce(announcement: Announcement) {
        state.update { it.copy(announcement = announcement, announcementSequence = it.announcementSequence + 1) }
    }

    private companion object {
        const val AI_PAUSE_MILLIS = 400L
    }
}
