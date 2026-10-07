# OX Play: plan

## Context
OX Play is tic-tac-toe for Android and the first game built on vinkit (`vinkit.tag`, newest release from JitPack,
never a local copy). It follows `../game-prompt-template.md` sections 2–8; what vinkit already provides is not
rewritten. Building it is also the test of the kit: gaps found here are fixed in vinkit and released as a new tag
before the game uses them.

## Confirmed decisions
| Key | Value |
|---|---|
| App name | OX Play (a string resource; can change any time) |
| Package / applicationId | `io.github.vinaooo.xo`; folder `~/Projects/xo`; repo `vinaooo/xo`, public, MIT |
| Variants | board 3×3 (3 in a row), 4×4 (4 in a row), 5×5 (4 in a row) |
| Opponent | AI easy (random), medium (win / block / center heuristics), hard (minimax), and local 2-player |
| Scoring | stats only (played / won / lost / drawn / streaks) per mode; no points, no ranking, no timer |
| Brand color | purple (`ThemeColor.PURPLE`) |
| Languages | pt-BR + en |
| Monetization | one banner on the game screen, placeholder first (vinkit `PlaceholderAdBanner`), AdMob later |

## Defaults (veto any)
1. **Mode** = board size × opponent: 12 modes. Stats are kept for the 9 AI modes; 2-player games aren't recorded.
2. **Turns:** the player is X vs the AI. Who starts alternates every new game (the AI opens half the games).
3. **Hard AI:** perfect on 3×3 (full minimax with memoization). On 4×4 and 5×5 a full search is too slow on a phone,
   so hard = alpha-beta to a fixed depth with a line-counting heuristic: very strong, not guaranteed unbeatable.
4. **Toolbar:** undo (vs AI it also takes back the AI's reply), redo, hint (the hard AI's move, highlighted, then
   a second tap plays it, as in Sudoku Trio). No notes, no stuck detection, no auto-complete.
5. **Settings → Game:** board size (segmented 3×3 / 4×4 / 5×5) and opponent (4 options: connected icon toggle group
   with name and description bouncing in, per the template). A change asks first if a game is in progress.
6. **Info area** (where other games show mode and clock): the mode ("4×4 · Hard") and whose turn it is
   ("Your turn", "OX Play is thinking", "O's turn" in 2-player); the AI answers after a short pause (~400 ms) so its
   move is visible.
7. **End of game:** win → vinkit `WinDialog` with a celebration; loss and draw → the same dialog with "You lost" /
   "Draw" and no celebration. The winning line is drawn across the board first.
8. **Look:** X and O drawn as Compose strokes that animate in (X two strokes, O a circle), X in `primary`, O in
   `tertiary`, contrast ≥ 3:1 tested in every palette; grid lines in `outlineVariant`.
9. **Seeded:** the AI's random choices (easy, ties) come from the game's seed, so a game replays exactly.
10. **TalkBack:** one node per cell ("row 2, column 3, X" / "empty"), a click to play; moves announced, the AI's
    included.

## vinkit gaps found already (fix in vinkit first, release 0.3.0)
- `GameStats` has no draws and the Scores screen assumes rankings: add `drawn` (+ `afterDraw()`), show it in the stats
  card, and let `ScoresScreen` run stats-only (no "Win a game to see your scores here", no score rows).
- Nothing else is known yet; whatever comes up while building goes to the same list.

## Modules
```
:app ─► :feature:game, :data, vinkit settings / scores / shell / ads / designsystem
:feature:game ─► :domain, vinkit shell / designsystem
:data ─► :domain, vinkit settings / scores
:domain   pure Kotlin: board, rules, AI, session, use cases (vinkit core for AppSettings, GameStats, GameCodec)
```
No `:feature:scores` or `:feature:settings`: the vinkit screens are wired in `:app` (game section composable in
`:feature:game`). No `build-logic`: vinkit plugins. Versions: vinkit's catalog.

## Domain design
- `Board(size, cells)`, `Mark { X, O }`, `GameMode(size, opponent)`, `GameState(board, mode, toMove, status, moves)`
  with `status` = playing / won(mark, line) / draw. All `@Serializable`.
- `Move.Place(cell)`; `GameEngine.apply` → `Applied` / `Rejected`; `legalMoves`, `isLegal`; win check by lines of the
  mode's length.
- `Opponent` strategies behind one interface: `RandomAi`, `HeuristicAi`, `MinimaxAi` (memoized on 3×3, alpha-beta
  with depth limit and heuristic on 4×4 / 5×5), all seeded.
- `UndoHistory` of board snapshots; vs AI an undo goes back to the player's last turn.
- `GameSession(seed, state, history)`; use cases: start (counts an unfinished AI game as a loss), play, finish
  (records stats), save / resume.
- Tests: rules per size, every winning line, draws, property tests (any legal sequence keeps the board valid; undo
  restores; same seed same game); hard AI never loses on 3×3 from any position (exhaustive), beats easy ≥ 95% on
  4×4 / 5×5; AI move time on 5×5 under 300 ms in the JVM test.

## Milestones (one PR each, after your approval)
1. Scaffold: repo, vinkit plugins and catalog, CI, wrapper, `CLAUDE.md`, empty app on the vinkit theme.
2. Domain core: board, rules, engine, session, undo, codec. (+ vinkit 0.3.0 for draws / stats-only.)
3. AI: easy, medium, hard, with the tests above and timing.
4. Data: saved game file, vinkit settings + scores wiring, game settings keys.
5. Game screen: board drawing and input, `GameFrame`, toolbar, end dialog, feedback, TalkBack.
6. Settings and Scores (vinkit screens + game section), navigation, ads placeholder, bug report.
7. Adaptive layouts, pt-BR, accessibility pass, screenshots, launcher icon.
8. Release prep: signing, Play workflow (off), privacy policy, store checklist.

## Verification
- Gate: `./gradlew ktlintCheck detekt lint test verifyRoborazziDebug koverVerify`, `:domain:pitest`.
- On your phone and the emulators: play every mode, undo vs AI, hint, rotation, both hands, tablet phone view,
  themes, both languages, Scores after wins / losses / draws, bug report (GitHub path only).
