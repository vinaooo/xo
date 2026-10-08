# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

OX Play is an open-source (MIT) tic-tac-toe for Android, to be published on Google Play: 3×3, 4×4 and 5×5 boards
(3, 4 and 4 in a row), against an AI (easy, medium, hard) or local 2-player. Kotlin, Jetpack Compose, Material 3
Expressive, Hilt. The UI is in pt-BR and English: every user-facing string lives in `values/strings.xml` and
`values-pt-rBR/strings.xml` of its module, never hard-coded. The user makes the product and design decisions, so ask
before choosing one. The plan, with the decisions and their defaults, is `PLAN.md`.

- applicationId and package `io.github.vinaooo.xo`; app name "OX Play" (`app_name`).
- It is the first game built on **vinkit** (`../vinkit`, github.com/vinaooo/vinkit): build-logic plugins (`vinkit.*`),
  the version catalog, theme, ads, bug report, settings, scores and the game screen shell come from it.
  - Always the newest published vinkit tag (`vinkit.tag` in `gradle.properties`), from JitPack; never a local
    `includeBuild` of vinkit (user's rule).
  - A kit gap found here is fixed in vinkit, released as a new tag, and then used here.

## Commands

```bash
./gradlew assembleDebug                          # build
./gradlew installDebug                           # build + install on the connected device
./gradlew ktlintCheck detekt                     # static analysis (ktlintFormat fixes formatting)
./gradlew test koverVerify                       # unit tests + coverage gates
./gradlew lint
./gradlew recordRoborazziDebug                   # re-record screenshot goldens after an intended UI change
./gradlew :domain:pitest                         # mutation testing (gate: 80% killed, 90% coverage)
```

- **Low RAM:** `gradle.properties` is sized for a low-RAM dev machine (2 GB Gradle heap, no parallel builds). Don't
  raise these values. Run the gate in pieces (`ktlintCheck detekt`, `test koverVerify`, `lint`): all of it in one
  invocation runs out of Metaspace. CI writes its own larger settings.
- Coverage: vinkit's filters leave out composables, generated code, `*Activity` and `*Application`.

## Architecture

### `:domain` (pure Kotlin, package `io.github.vinaooo.xo.domain`)

- **Model:** `Mark` (X, O), `BoardSize` (THREE 3 in a row, FOUR 4, FIVE 4), `Opponent` (EASY, MEDIUM, HARD,
  TWO_PLAYER), `GameMode(size, opponent)` with a stable `key` ("FIVE_HARD") for vinkit's stats; `Board` (cells row by
  row, null = empty); `Lines` (every winning run per size, and the runs through each cell, computed once);
  `GameState(board, mode, toMove, firstMover, status, moves)`, `GameStatus` Playing / Won(mark, line) / Draw. All
  `@Serializable`. Against the AI the player is X (`GameMode.HUMAN`).
- **Engine:** `GameEngine.newGame/apply/isLegal/legalMoves`; one move type (`Move.Place`), so no per-move rule objects.
  A placement checks only the lines through its cell.
- **Session:** `GameSession(seed, state, undos, redos)` keeps whole-state snapshots (a board is at most 25 cells).
  Against the AI, undo and redo stop only on the player's turn, so the AI's reply goes with the player's move; when
  the AI opened, its first move can't be undone. A finished game can't be undone. `GameSession.codec` is vinkit's
  `GameCodec` for bug reports.
- **AI (`ai`):** `Ai.move(state, random)` picks a cell for the side to move; `aiFor(opponent)`. `GameSession.random()`
  is `Random(seed * 31 + moves)`: never change it, or saved games and bug reports replay different AI moves.
  - `RandomAi` (easy): any empty cell.
  - `HeuristicAi` (medium): win, else block, else a center cell, else any. Blind to forks.
  - `MinimaxAi` (hard): negamax with alpha-beta and a per-search transposition table (only exact values cached).
    Full depth on 3×3 (never loses); on 4×4 depth 6 (full once ≤ 9 cells are empty), on 5×5 depth 4 (6 once ≤ 9
    empty), with an open-line evaluation (a line holding only one side's marks scores 1, 10, 100, 1000 by count).
    Perfect play draws the bigger boards, so many moves tie; ties go to the move leaving the most open lines, which
    is what beats a player who errs (without it, hard drew 13 of 30 games against random play on 4×4; now 1).
    Moves are searched wins first, then blocks, then by open lines (`orderedMoves`).
  - With at most one mark on 4×4 / 5×5, hard takes a free center cell without searching (`obviousMove`): that widest
    search took ~650 ms on the user's Moto (XT2125, release build compiled `speed-profile`) only to pick a center.
  - Measured on that phone (release, `speed-profile`): slowest hard move 420 ms on 4×4 and 396 ms on 5×5, the first
    searched moves; later moves under 250 ms. The ViewModel thinks during its 400 ms pause, so that's the wait.
  - Tests never time the AI: CI's runners are slower than a phone, and a 300 ms limit failed every CI run from
    milestone 3 to 7. `AiStrengthTest` caps the positions hard visits per answer instead (`MinimaxAi.searchCost`:
    80,000 on 4×4, 35,000 on 5×5, just above today's search); speed is measured on the phone. Against easy over 30 games: 28/30 wins on 3×3,
    29/30 on 4×4, 30/30 on 5×5, never a loss.
- **Use cases:** `StartNewGame(mode?)` (Settings' mode by default; the opener alternates through
  `GameSettings.nextFirstMover`; an AI game in progress that it replaces counts as a loss; 2-player games are never
  recorded), `ResumeGame`, `SaveGame`, `FinishGame` (the player's win / loss / draw into vinkit's stats under
  `GameMode.key`, then clears the save). Repositories: `SavedGameRepository`, `GameSettingsRepository`, `SeedSource`,
  and vinkit's `StatsRepository`; fakes in `testFixtures`.
- **Tests:** `AiStrengthTest` plays whole games (never loses on 3×3 against every possible reply, beats easy, timing);
  it's slow, so Pitest leaves it out, and `AiTest` pins the same code with exact checks (search value = plain negamax
  on every 3×3 position, best-value moves, ordering, evaluation against a plain count).
- **Tests:** property tests over random legal games on every mode (board valid, `legalMoves` agrees with `isLegal`,
  undo all + redo all comes back).

### `:data`

- `FileSavedGameRepository` (`files/saved_game.json`, a `{version, session}` envelope, temp file + rename; corrupt,
  invalid or unknown-version files are discarded), `DataStoreGameSettingsRepository` (`board_size`, `opponent`,
  `next_first_mover`) in the same Preferences DataStore (`settings`) as vinkit's `DataStoreAppSettingsRepository`,
  each touching only its own keys. `DataModule` provides those, vinkit's `ScoresDatabase` + `RoomStatsRepository`
  (stats only: OX Play has no scores), and the brand color (`BRAND_COLOR`, purple) as `AppSettings`' default.
- `:app/di/UseCaseModule` assembles the use cases.

### `:feature:game`

- **`GameViewModel`** (one `StateFlow<GameUiState>`, sealed `GameIntent`: Place, Undo, Redo, Hint, NewGame):
  resumes the saved game or starts one; a Settings mode change (collected, first value dropped) starts a game in it.
  After any placement it announces it, plays `FeedbackEvent.MOVE`, then finishes (stats, clear save, `WIN` sound
  when the player or either 2-player side won) or saves, and lets the AI answer after a 400 ms pause on the injected
  `@AiDispatcher`; taps wait meanwhile (`canPlay`). Undo and redo cancel a pending answer or hint search; undo works
  while the AI thinks. Hint: first tap shows hard's move for the side to move, second tap plays it.
- **Screen:** vinkit `GameSurface` + `GameFrame`. Info = mode ("4×4 · Hard") over whose turn it is ("Your turn",
  "Thinking…", "X's turn") or the result, one TalkBack item. Toolbar: undo, redo, hint (a check while a hint shows);
  menu: new game. The end dialog (vinkit `WinDialog`) waits 900 ms so the winning line is seen struck; the player's win
  (or any 2-player win) gets a celebration, a loss or draw doesn't.
- **Board (`XoBoard` + `HandDrawn.kt` + `SketchbookPaper.kt`):** drawn by hand (user's choice among five prototypes) on
  a sketchbook page (user's choice among notebook, graph, sketchbook, sticky note; notebook was tried first): an
  off-white page (`surfaceContainerLow`) with a soft shadow and a light grain (three batches of fixed specks, one
  `drawPoints` each, cheap to redraw during animations). Then a wavering pen grid in
  `onSurfaceVariant`, X (`primary`) and O (`tertiary`) in four shapes each, picked by the cell
  (`(cell * 7 + mark) % 4`) so a board looks varied and a mark never changes shape, and the winning line struck with
  the same pen past the end cells. X shapes (user tuned: "the middle ground"): straight; tilted 8° with a shorter
  second stroke; tilted -8°, bowed both ways, crossing off center; wide and flat with a small end flick. O shapes: an
  overshooting loop, a tilted open oval, a 1¼-turn spiral, a lopsided egg. A new mark draws its strokes in (250 ms)
  and pops in from 70% size on a bouncy spring (damping 0.45). `BoardGeometry` (pure) maps taps to cells. One
  invisible TalkBack node per cell, row by row ("row 2, column 3, X" / "empty" / ", suggested"), with a click only on
  empty cells while the player may play. `MarkColorsTest` keeps X and O at ≥ 3:1 against the surface in every palette.
- **Settings** (`settings/`): vinkit's `SettingsScreen` with a Game section (board: segmented 3×3 / 4×4 / 5×5;
  opponent: vinkit `IconChoice` with a note per opponent). A change while a game is in progress asks first
  (`PendingMode`): vinkit's "counts as a loss" text against the AI, "will be lost" in 2-player (not recorded). The
  privacy policy is `privacy_policy_url` (`/xo/privacy.html#en` / `#pt-br` on vinaooo.github.io, written in release
  prep).
- **Scores** (`scores/`): vinkit's `ScoresScreen(ranked = false)` (stats with draws and losses); `XoScoresViewModel`
  subclasses vinkit's `ScoresViewModel` with the 9 AI mode keys in board-then-opponent order and an empty
  `ScoreRepository` (OX Play has no scores).
- **Bug report:** `GameSurface` with `REPORT_TARGET` (`vrpedrinho+xo@gmail.com`, user's choice, or an issue on
  `vinaooo/xo`); `gameReport` adds a settings line, a game line, the `GameCodec` state and `game.json`.
- **`DebugGameActivity`** (debug builds): `adb shell am start -S -n io.github.vinaooo.xo/.debug.DebugGameActivity
  --es game near_win|near_loss|five_full`, or `--es state <code>` (a report's "State:" block), or `--es load
  game.json` (pushed to `/sdcard/Android/data/io.github.vinaooo.xo/files/`).
- `:app`: `OxPlayApp` (type-safe NavHost: game, Scores, Settings; the banner only under the game, which consumes the
  navigation-bar inset), `di/GameModule` (vinkit's `AndroidGameFeedback`, `@AiDispatcher` = `Dispatchers.Default`),
  `di/AdsModule` (vinkit's `AdMobBanner` and `DefaultAdConsent`, IDs from `BuildConfig`: Google's test IDs in debug
  and until real ones are in `local.properties`; debug builds simulate the EEA). `MainActivity` gathers consent once
  per launch; Settings shows "Privacy options" when UMP requires it. App tests replace `AdsModule` with
  `FakeAdsModule` (`src/sharedTest`); `AdBannerGameScreenOnlyTest` runs the real app under Hilt (banner on the game
  only, consent gathered once).
- **Ads on devices:** the user's phone has no Google Play services: no ads, no consent form there. Check them on
  `Pixel_9a_Android_16` (checked: EEA test form, test banner, Settings → Privacy options). Its airplane mode was left
  on from Sudoku's store screenshots: `cmd connectivity airplane-mode disable` on the emulator.

### Pitest

- Suspend functions leave coroutine bookkeeping mutants nothing can kill; `avoidCallsTo kotlin.ResultKt` drops the
  `throwOnFailure` ones. Prefer non-suspend logic in the domain where it reads as well.

### Look

- **Launcher icon:** adaptive vector, white strokes on brand purple (`#794F81`, vinkit's `PurpleColors.light.primary`):
  a 3×3 grid (cells 16 wide, 30–78, every stroke end inside the 66dp safe circle) with X top left and in the middle
  and O top right (user's choice: "mini 3×3 board"). A separate monochrome layer with the same strokes in black for
  themed icons. `LauncherIconScreenshotTest` keeps a golden of the icon and two themed tints.
- **Screenshots** (Roborazzi, `src/test/screenshots/`, verified on every `test`): `GameScreenScreenshotTest` shoots
  3×3 light, 4×4 dark with a hint, a won 5×5 with its line struck, left-handed landscape, tablet phone view on the
  right, and pt-BR, with dynamic color off (brand purple) and the clock held so the end dialog isn't up yet.
- Checked on the `Pixel_Tablet_Android_16` emulator: landscape layout, the 320dp ad slot, Settings in two columns,
  phone view (board, toolbar under it, on the chosen side) and the whole UI in pt-BR (`cmd locale set-app-locales`).

## Release

- **Audience:** 13 and older (user's choice): not in Google Play's Families program; the listing must not market to
  children.
- **Privacy policy:** `xo/privacy.html` in the public `vinaooo.github.io` repo (default branch `main`, Pages serves
  `master`: push the same commit to both, check with `curl`, force a rebuild with `gh api -X POST
  repos/vinaooo/vinaooo.github.io/pages/builds`). It lists the release build's real permissions (internet, network
  state, vibration, AD_ID, the ad services ones, wake lock, foreground service): update it when they change.
- `release.yml`: off until `PLAY_UPLOAD_ENABLED`; secrets in the README.

## Device testing

- The user's phone (Moto XT2125, `nio_retcn`, wireless adb, no Google Play services) and the emulators
  `Pixel_9a_Android_16` (Play Store image), `Pixel_Tablet_Android_16`, `Solo_API_26` (minSdk). Taps from a script:
  `uiautomator dump` gives each TalkBack node's bounds ("row 1, column 1, empty", "Settings").
- Timing: a temporary `Log` line around `ai.move` in a release build (`assembleRelease`, `zipalign -p 4`, `apksigner`
  with `~/.android/debug.keystore`, `adb install -r`, `cmd package compile -m speed-profile -f
  io.github.vinaooo.xo`), never committed.

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per
milestone (`PLAN.md`). Commit, push and open PRs only when the user asks.
