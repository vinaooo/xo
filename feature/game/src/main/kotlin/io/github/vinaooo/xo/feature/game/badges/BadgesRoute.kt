package io.github.vinaooo.xo.feature.game.badges

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinaooo.vinkit.achievements.Badge
import io.github.vinaooo.vinkit.achievements.BadgesScreen
import io.github.vinaooo.vinkit.achievements.R as AchievementsR
import io.github.vinaooo.xo.domain.model.Achievement
import io.github.vinaooo.xo.domain.model.Ladder
import io.github.vinaooo.xo.feature.game.R

@Composable
fun BadgesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: BadgesViewModel = hiltViewModel()) {
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    BadgesScreen(Achievement.entries.map { badge(it) }, unlocked, onBack, modifier)
}

/** A badge in vinkit's terms: stored by its name, shown in the UI language. */
@Composable
internal fun badge(achievement: Achievement): Badge {
    val ladder = achievement.ladder
    return if (ladder != null) {
        val (name, note) = LADDER_TEXT.getValue(ladder)
        Badge(
            achievement.name,
            pluralStringResource(name, achievement.count, achievement.count),
            pluralStringResource(note, achievement.count, achievement.count),
        )
    } else {
        val (name, note) = BADGE_TEXT.getValue(achievement)
        Badge(achievement.name, stringResource(name), stringResource(note))
    }
}

private val LADDER_TEXT = mapOf(
    Ladder.PLAYED to (AchievementsR.plurals.vinkit_badge_played to R.plurals.badge_played_note),
    Ladder.WON to (AchievementsR.plurals.vinkit_badge_won to R.plurals.badge_won_note),
    Ladder.STREAK to (AchievementsR.plurals.vinkit_badge_streak to R.plurals.badge_streak_note),
    Ladder.DAYS to (R.plurals.badge_days_name to R.plurals.badge_days_note),
)

private val BADGE_TEXT = mapOf(
    Achievement.WIN_THREE to (R.string.badge_win_three_name to R.string.badge_win_three_note),
    Achievement.WIN_FOUR to (R.string.badge_win_four_name to R.string.badge_win_four_note),
    Achievement.WIN_FIVE to (R.string.badge_win_five_name to R.string.badge_win_five_note),
    Achievement.WIN_EVERY_SIZE to (R.string.badge_win_every_size_name to R.string.badge_win_every_size_note),
    Achievement.BEAT_HARD to (R.string.badge_beat_hard_name to R.string.badge_beat_hard_note),
    Achievement.DRAW_HARD to (R.string.badge_draw_hard_name to R.string.badge_draw_hard_note),
    Achievement.QUICK_WIN to (R.string.badge_quick_win_name to R.string.badge_quick_win_note),
    Achievement.WIN_SECOND to (R.string.badge_win_second_name to R.string.badge_win_second_note),
    Achievement.NO_HINTS to (AchievementsR.string.vinkit_badge_no_hints to R.string.badge_no_hints_note),
    Achievement.NO_UNDO to (R.string.badge_no_undo_name to R.string.badge_no_undo_note),
)
