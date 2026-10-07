# OX Play

Tic-tac-toe for Android: 3×3, 4×4 and 5×5 boards, against the phone (easy, medium, hard) or a friend on the same
phone. Open source (MIT). Kotlin, Jetpack Compose, Material 3 Expressive, Hilt.

Built on [vinkit](https://github.com/vinaooo/vinkit), which brings the build setup, settings, scores, toolbar, layouts,
ads and bug report; this repo holds the game itself.

## Build

JDK 21 and Android SDK Platform 37.

```bash
./gradlew assembleDebug
./gradlew installDebug
```

The vinkit release is `vinkit.tag` in `gradle.properties` (always the newest tag, from JitPack).

## Release setup

Release builds read their AdMob IDs and upload key from `local.properties` (`vinkit.ads.appId`,
`vinkit.ads.bannerId`, `vinkit.ads.testDeviceIds`, `vinkit.signing.*`) or CI (`VINKIT_ADS_*`, `VINKIT_SIGNING_*`).
Without them, release builds use Google's test ads and stay unsigned.
