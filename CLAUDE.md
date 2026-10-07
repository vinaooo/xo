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
  - Measured (JVM): slowest hard move 191 ms on 4×4, 61 ms on 5×5. Against easy over 30 games: 28/30 wins on 3×3,
    29/30 on 4×4, 30/30 on 5×5, never a loss.
- **Tests:** `AiStrengthTest` plays whole games (never loses on 3×3 against every possible reply, beats easy, timing);
  it's slow, so Pitest leaves it out, and `AiTest` pins the same code with exact checks (search value = plain negamax
  on every 3×3 position, best-value moves, ordering, evaluation against a plain count).
- **Tests:** property tests over random legal games on every mode (board valid, `legalMoves` agrees with `isLegal`,
  undo all + redo all comes back).

## Git

Only `master` is long-lived: branch from it and open PRs against it; the user merges with merge commits. One PR per
milestone (`PLAN.md`). Commit, push and open PRs only when the user asks.
