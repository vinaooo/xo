package io.github.vinaooo.xo.domain.repository

import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    suspend fun load(): GameSession?

    suspend fun save(session: GameSession)

    suspend fun clear()
}

/** OX Play's own settings, beside vinkit's `AppSettings`. */
data class GameSettings(
    /** The mode of the next new game; the game in progress keeps its own. */
    val mode: GameMode = GameMode.DEFAULT,
    /** Who opens the next game: it alternates, so the AI opens every other one. */
    val nextFirstMover: Mark = Mark.X,
)

interface GameSettingsRepository {
    val settings: Flow<GameSettings>

    suspend fun update(transform: (GameSettings) -> GameSettings)
}

fun interface SeedSource {
    fun nextSeed(): Long
}
