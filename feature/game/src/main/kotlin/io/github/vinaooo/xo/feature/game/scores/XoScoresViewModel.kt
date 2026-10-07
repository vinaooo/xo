package io.github.vinaooo.xo.feature.game.scores

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vinaooo.vinkit.core.Ranking
import io.github.vinaooo.vinkit.core.ScoreRecord
import io.github.vinaooo.vinkit.core.ScoreRepository
import io.github.vinaooo.vinkit.core.StatsRepository
import io.github.vinaooo.vinkit.scores.ScoresViewModel
import io.github.vinaooo.xo.domain.model.GameMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** vinkit's Scores, stats only: a tab per AI mode played, in board then opponent order. */
@HiltViewModel
class XoScoresViewModel @Inject constructor(stats: StatsRepository) :
    ScoresViewModel(NoScores, stats, GameMode.ALL.filter { it.isVsAi }.map { it.key })

/** OX Play keeps wins, losses and draws, no scores. */
private object NoScores : ScoreRepository {
    override fun observeTopScores(mode: String, ranking: Ranking, limit: Int): Flow<List<ScoreRecord>> =
        flowOf(emptyList())

    override suspend fun add(record: ScoreRecord) = Unit
}
