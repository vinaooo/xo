package io.github.vinaooo.xo.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.vinaooo.vinkit.core.AppSettings
import io.github.vinaooo.vinkit.core.ThemeMode
import io.github.vinaooo.vinkit.settings.DataStoreAppSettingsRepository
import io.github.vinaooo.xo.domain.model.BoardSize
import io.github.vinaooo.xo.domain.model.GameMode
import io.github.vinaooo.xo.domain.model.Mark
import io.github.vinaooo.xo.domain.model.Opponent
import io.github.vinaooo.xo.domain.repository.GameSettings
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreGameSettingsRepositoryTest {
    @TempDir
    lateinit var dir: File

    private val scope = TestScope(StandardTestDispatcher())

    private val store by lazy {
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { File(dir, "settings.preferences_pb") }
    }

    @Test
    fun `first launch reads the defaults`() = scope.runTest {
        DataStoreGameSettingsRepository(store).settings.first() shouldBe GameSettings()
    }

    @Test
    fun `the mode and the next opener are persisted`() = scope.runTest {
        val changed = GameSettings(GameMode(BoardSize.FIVE, Opponent.TWO_PLAYER), nextFirstMover = Mark.O)
        DataStoreGameSettingsRepository(store).update { changed }

        DataStoreGameSettingsRepository(store).settings.first() shouldBe changed
    }

    @Test
    fun `game and vinkit settings share the file without touching each other`() = scope.runTest {
        val app = DataStoreAppSettingsRepository(store)
        app.update { it.copy(themeMode = ThemeMode.DARK) }
        DataStoreGameSettingsRepository(store).update { it.copy(nextFirstMover = Mark.O) }

        app.settings.first() shouldBe AppSettings(themeMode = ThemeMode.DARK)
        DataStoreGameSettingsRepository(store).settings.first().nextFirstMover shouldBe Mark.O
    }

    @Test
    fun `a value from a newer version reads as the default`() = scope.runTest {
        store.edit { it[stringPreferencesKey("board_size")] = "SEVEN" }

        DataStoreGameSettingsRepository(store).settings.first() shouldBe GameSettings()
    }
}
