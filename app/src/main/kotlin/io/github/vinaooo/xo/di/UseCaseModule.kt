package io.github.vinaooo.xo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.xo.domain.repository.GameSettingsRepository
import io.github.vinaooo.xo.domain.repository.SavedGameRepository
import io.github.vinaooo.xo.domain.repository.SeedSource
import io.github.vinaooo.xo.domain.rules.GameEngine
import io.github.vinaooo.xo.domain.usecase.FinishGame
import io.github.vinaooo.xo.domain.usecase.ResumeGame
import io.github.vinaooo.xo.domain.usecase.SaveGame
import io.github.vinaooo.xo.domain.usecase.StartNewGame

/** The domain's classes, which carry no DI annotations, assembled for Hilt. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun engine() = GameEngine()

    @Provides
    fun startNewGame(
        savedGames: SavedGameRepository,
        gameSettings: GameSettingsRepository,
        stats: StatsRepository,
        seeds: SeedSource,
        engine: GameEngine,
    ) = StartNewGame(savedGames, gameSettings, stats, seeds, engine)

    @Provides
    fun resumeGame(savedGames: SavedGameRepository) = ResumeGame(savedGames)

    @Provides
    fun saveGame(savedGames: SavedGameRepository) = SaveGame(savedGames)

    @Provides
    fun finishGame(savedGames: SavedGameRepository, stats: StatsRepository) = FinishGame(savedGames, stats)
}
