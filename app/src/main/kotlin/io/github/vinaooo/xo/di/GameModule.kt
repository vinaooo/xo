package io.github.vinaooo.xo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.vinkit.shell.AndroidGameFeedback
import io.github.vinaooo.vinkit.shell.GameFeedback
import io.github.vinaooo.xo.feature.game.AiDispatcher
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object GameModule {
    /** One per app: it loads the sounds once. */
    @Provides
    @Singleton
    fun feedback(@ApplicationContext context: Context): GameFeedback = AndroidGameFeedback(context)

    @Provides
    @AiDispatcher
    fun aiDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
