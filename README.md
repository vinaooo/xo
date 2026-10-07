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

Release builds read their AdMob IDs and upload key from `local.properties` or CI; without them they use Google's
test ads and stay unsigned. Debug builds always use test ads.

| `local.properties` | CI secret | What |
|---|---|---|
| `vinkit.ads.appId` | `VINKIT_ADS_APP_ID` | AdMob app ID (`ca-app-pub-…~…`) |
| `vinkit.ads.bannerId` | `VINKIT_ADS_BANNER_ID` | banner ad unit (`ca-app-pub-…/…`) |
| `vinkit.ads.testDeviceIds` | — | hashed IDs of your devices (from logcat), which always get test ads |
| `vinkit.signing.storeFile`, `.storePassword`, `.keyAlias`, `.keyPassword` | `VINKIT_SIGNING_STORE_PASSWORD`, `VINKIT_SIGNING_KEY_ALIAS`, `VINKIT_SIGNING_KEY_PASSWORD`, `VINKIT_UPLOAD_KEYSTORE_BASE64` | upload key |
| — | `PLAY_SERVICE_ACCOUNT_JSON` | Play Console service account for uploads |

`.github/workflows/release.yml` builds the signed bundle and uploads it to Google Play on a `vX.Y.Z` tag (internal
track) or by hand. It is off until the repository variable `PLAY_UPLOAD_ENABLED` is `true`, and it stops before
building if any secret above is missing. The version code is the commit count; the version name is the latest
`vX.Y.Z` tag.

The privacy policy is at https://vinaooo.github.io/xo/privacy.html (English and Portuguese).
