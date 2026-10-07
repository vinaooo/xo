package io.github.vinaooo.xo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.repository.GameSettings
import io.github.vinaooo.xo.domain.repository.GameSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * OX Play's settings, in the same Preferences DataStore as vinkit's (its own keys only). A value this version doesn't
 * know reads as the default.
 */
class DataStoreGameSettingsRepository(private val dataStore: DataStore<Preferences>) : GameSettingsRepository {
    override val settings: Flow<GameSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        dataStore.edit { prefs ->
            val settings = transform(prefs.toSettings())
            prefs[BOARD_SIZE] = settings.mode.size.name
            prefs[OPPONENT] = settings.mode.opponent.name
            prefs[NEXT_FIRST_MOVER] = settings.nextFirstMover.name
        }
    }

    private fun Preferences.toSettings(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            mode = GameMode(
                size = enumOrDefault(this[BOARD_SIZE], defaults.mode.size),
                opponent = enumOrDefault(this[OPPONENT], defaults.mode.opponent),
            ),
            nextFirstMover = enumOrDefault(this[NEXT_FIRST_MOVER], defaults.nextFirstMover),
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val BOARD_SIZE = stringPreferencesKey("board_size")
        val OPPONENT = stringPreferencesKey("opponent")
        val NEXT_FIRST_MOVER = stringPreferencesKey("next_first_mover")
    }
}
