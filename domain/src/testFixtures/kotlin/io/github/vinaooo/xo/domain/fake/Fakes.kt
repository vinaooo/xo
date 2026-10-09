package io.github.vinaooo.xo.domain.fake

import io.github.vinaooo.vinkit.core.AchievementProgress
import io.github.vinaooo.vinkit.core.AchievementRepository
import io.github.vinaooo.vinkit.core.GameStats
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.xo.domain.repository.GameSettings
import io.github.vinaooo.xo.domain.repository.GameSettingsRepository
import io.github.vinaooo.xo.domain.repository.SavedGameRepository
import io.github.vinaooo.xo.domain.repository.SeedSource
import io.github.vinaooo.xo.domain.session.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSavedGameRepository(var saved: GameSession? = null) : SavedGameRepository {
    override suspend fun load(): GameSession? = saved

    override suspend fun save(session: GameSession) {
        saved = session
    }

    override suspend fun clear() {
        saved = null
    }
}

class FakeGameSettingsRepository(initial: GameSettings = GameSettings()) : GameSettingsRepository {
    val current = MutableStateFlow(initial)

    override val settings: Flow<GameSettings> = current

    override suspend fun update(transform: (GameSettings) -> GameSettings) {
        current.value = transform(current.value)
    }
}

class FakeStatsRepository(initial: Map<String, GameStats> = emptyMap()) : StatsRepository {
    val stats = MutableStateFlow(initial)

    override fun observe(mode: String): Flow<GameStats> = stats.map { it[mode] ?: GameStats() }

    override fun observePlayedModes(): Flow<Set<String>> = stats.map { all -> all.filterValues { it.played > 0 }.keys }

    override suspend fun update(mode: String, transform: (GameStats) -> GameStats) {
        stats.value = stats.value + (mode to transform(stats.value[mode] ?: GameStats()))
    }
}

/** Seeds 1, 2, 3, … */
class FakeSeedSource : SeedSource {
    private var next = 0L

    override fun nextSeed(): Long = ++next
}

class FakeAchievementRepository(initial: AchievementProgress = AchievementProgress()) : AchievementRepository {
    val current = MutableStateFlow(initial)

    override val progress: Flow<AchievementProgress> = current

    override suspend fun update(transform: (AchievementProgress) -> AchievementProgress) {
        current.value = transform(current.value)
    }
}
