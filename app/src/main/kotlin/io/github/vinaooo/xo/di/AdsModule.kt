package io.github.vinaooo.xo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.vinaooo.vinkit.ads.AdBannerProvider
import io.github.vinaooo.vinkit.ads.PlaceholderAdBanner

/**
 * The ad banner. ponytail: vinkit's placeholder until the AdMob app and its ad unit exist (release prep); then
 * `AdMobBanner` with `DefaultAdConsent`, as in vinkit's README, and Settings' privacy options.
 */
@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    @Provides
    fun adBanner(): AdBannerProvider = PlaceholderAdBanner()
}
