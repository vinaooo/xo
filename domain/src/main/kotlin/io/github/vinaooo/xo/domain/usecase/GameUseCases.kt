package io.github.vinaooo.xo.domain.usecase

import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.GameStatus
import io.github.vinaooo.xo.domain.repository.GameSettingsRepository
import io.github.vinaooo.xo.domain.repository.SavedGameRepository
import io.github.vinaooo.xo.domain.repository.SeedSource
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.session.GameSession
import kotlinx.coroutines.flow.first

/**
 * Starts a game in [GameMode] (Settings' mode by default), opened by whoever's turn it is to open. A game against the
 * AI it replaces, with moves made and not over, counts as a loss; 2-player games aren't recorded.
 */
class StartNewGame(
    private val savedGames: SavedGameRepository,
    private val gameSettings: GameSettingsRepository,
    private val stats: StatsRepository,
    private val seeds: SeedSource,
    private val engine: GameEngine,
) {
    suspend operator fun invoke(mode: GameMode? = null): GameSession {
        savedGames.load()?.takeIf { it.isInProgress && it.state.mode.isVsAi }?.let {
            stats.update(it.state.mode.key, GameStats::afterLoss)
        }
        val settings = gameSettings.settings.first()
        val first = settings.nextFirstMover
        gameSettings.update { it.copy(nextFirstMover = first.other) }
        val session = GameSession(seeds.nextSeed(), engine.newGame(mode ?: settings.mode, first))
        savedGames.save(session)
        return session
    }
}

/** The saved game, if a game was left on screen: in progress, or not yet begun. */
class ResumeGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(): GameSession? = savedGames.load()
}

class SaveGame(private val savedGames: SavedGameRepository) {
    suspend operator fun invoke(session: GameSession) = savedGames.save(session)
}

/**
 * Records a finished game against the AI in its mode's stats, from the player's side, and clears the save. A
 * 2-player game is only cleared.
 */
class FinishGame(private val savedGames: SavedGameRepository, private val stats: StatsRepository) {
    suspend operator fun invoke(session: GameSession) {
        val state = session.state
        check(state.isOver) { "Only a finished game can be recorded" }
        if (state.mode.isVsAi) {
            val record: (GameStats) -> GameStats = when (val status = state.status) {
                is GameStatus.Won -> if (status.mark == GameMode.HUMAN) GameStats::afterWin else GameStats::afterLoss
                else -> GameStats::afterDraw
            }
            stats.update(state.mode.key, record)
        }
        savedGames.clear()
    }
}
