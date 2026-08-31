# Architecture & Flow Overview

This is a map of how the project's pieces connect: what talks to what, and in what order. It
covers the whole system as of 2026-08-31 — the terminal engine, the LibGDX graphical client, and
the network server — not just one phase of it. `CLAUDE.md` at the repo root still has good detail
on the terminal-only `Menu`/`Router` navigation pattern; this document does not repeat that, it
covers everything CLAUDE.md predates: the graphical client, the network layer, and how the pieces
that already existed connect to both.

## The shape of it

One model (`model/`, `data/`) is driven by three different front doors:

```
                    ┌─────────────────────┐
                    │   model/ + data/     │   ← one game engine, one set of
                    │  (plants, zombies,   │     JSON templates, one User/account
                    │   board, quests...)  │     shape, shared by everything below
                    └──────────┬───────────┘
             ┌──────────────────┼──────────────────┐
             │                  │                   │
      ┌──────▼──────┐   ┌───────▼────────┐   ┌──────▼───────┐
      │  Terminal    │   │  LibGDX client  │   │  Network      │
      │  (Main.java, │   │  (view/gdx/,    │   │  server       │
      │  view/*Menu*,│   │  Gradle task    │   │  (network/    │
      │  controller/)│   │  runGdx)        │   │  server/,     │
      └──────────────┘   └────────┬────────┘   │  runServer)   │
                                   │            └───────▲───────┘
                                   └── network/client ───┘
                                       (ClientSession)
```

- The **terminal** front end is a blocking `Scanner` loop over `Menu`/`Router` — see CLAUDE.md.
- The **graphical client** (`view/gdx/`) is a LibGDX/LWJGL3 desktop app, started with
  `./gradlew runGdx`. It reuses the exact same model classes and, for anything account-related,
  the exact same `UserManager`/`ClientSession` the terminal front end uses.
- The **server** (`network/server/`, started with `./gradlew runServer`) is the authority for
  accounts and for networked matches. Both front ends are clients of it.

Neither front end can do anything account-related — sign up, log in, change a password, see the
leaderboard — without reaching the server. There is no offline fallback for auth in either front
end (see "Terminal vs. server-authoritative", below).

## 1. Startup and data loading

Both front ends call the same `App.initData()` (`model/core/App.java`):

```
App.initData()
  → GameDataManager.initAllData()      reads plants.json / Zombies.json / Quests.json
                                        (via DataPath + JsonSerializer) into
                                        GameDataManager.plantRepository / zombieRepository /
                                        questRepository (public static fields)
  → UserManager.getInstance().restoreSession()   tries a saved "stay logged in" token
  → ClientSession.getInstance().connect()        opens the socket to the server
  → if the restored session is real → jump straight to the main menu
```

`DataPath` resolves the JSON file paths by trying `src/data/database/<file>` then
`data/database/<file>` and using whichever exists — so, unlike what CLAUDE.md says, the process no
longer has to be launched with `cwd = src/`; it now tolerates being launched from the repo root
too (relevant for `./gradlew runGdx`, whose working directory is the project root).

The terminal front end calls this from `view/AppView.java`; the graphical client calls it from
`view/gdx/screens/LoadingScreen.java` — deliberately the same call, so the graphical build ends up
with the same repositories and the same restored session the terminal build would have.

## 2. The network layer

### Wire format

One TCP socket, one `NetworkMessage` per line (newline-delimited JSON, via Gson). A message is
`{type, id, payload}`:

- **A request** carries a random UUID `id`. The client blocks on a `CompletableFuture` keyed by
  that `id` until a reply with the same `id` arrives (or a 5s timeout) — see
  `network/client/NetworkClient.java`.
- **A reply** echoes the same `id` back with a new `type`/`payload`.
- **An event** (a server push, not a reply to anything) has `id = null`. `NetworkClient` routes
  anything with a null `id` to registered listeners instead of completing a future.

`network/protocol/MessageType.java` lists every message type, grouped:

| Group | Requests | Events / Responses |
|---|---|---|
| Auth | REGISTER_REQUEST, LOGIN_REQUEST, TOKEN_LOGIN_REQUEST, LOGOUT_REQUEST, SECURITY_QUESTION_REQUEST, PASSWORD_RESET, RENAME_REQUEST, PROFILE_UPDATE | REGISTER_RESPONSE, LOGIN_RESPONSE, SECURITY_QUESTION_RESPONSE, RENAME_RESPONSE, PROFILE_RESPONSE |
| Matchmaking | MATCHMAKING_REQUEST, MATCHMAKING_CANCEL, MATCH_INVITE, MATCH_INVITE_DECISION | MATCH_FOUND, MATCH_INVITE_EVENT (pushed) |
| Gameplay | GAME_ACTION, REACTION | MATCH_STATE_UPDATE, MATCH_ENDED, REACTION_EVENT (all pushed) |
| Leaderboard | LEADERBOARD_REQUEST, SCORE_SUBMISSION | LEADERBOARD_RESPONSE, SCORE_RESPONSE |
| Generic | PING | PONG, ACK, ERROR |

`network/protocol/Payloads.java` has the record for every one of these. Notably, `Profile` and
`ProfileUpdate` carry the player's whole save as an opaque `gameData` (`JsonElement`) — the server
does not know or care about its shape, it's the same `User` object Gson would serialize on the
terminal side.

### Client side

`network/client/`:

- **`NetworkClient`** — the raw transport. Owns the socket, a reader thread, the
  request/future map, and the event-listener list.
- **`ClientSession`** — the one connection every screen/controller shares (a singleton). Wraps
  `NetworkClient` with the actual API calls (`login`, `register`, `requestMatchmaking`,
  `sendAction`, `requestLeaderboard`, ...), and keeps the last-known `Profile` and
  `MatchFound` up to date from server pushes.
- **`data/persistence/UserManager`** — the account layer every screen and controller actually
  calls. Since "Phase 3" it is a thin wrapper: every method (`loginUser`, `registerUser` +
  `setSecurityQuestionForLatestUser`, `changeUsername/Nickname/Email/Password`,
  `updateCurrentUserGameState`, password recovery) calls through `ClientSession`. The local
  `Users.json` file is still written on every successful call, but purely as a mirror — nothing
  reads it back for authentication anymore. **There is no offline auth path**: `loginUser` throws
  immediately if `ClientSession.connect()` fails.

Every one of those `UserManager` calls blocks on a socket round trip. On the graphical client this
matters: calling them straight from a button handler would freeze the whole window for as long as
the request takes. Every screen that touches the network runs it through
`BaseScreen.runAsync(...)` — a small helper (worker thread + `Gdx.app.postRunnable`) that every
account/save-related screen (login, sign-up, profile, greenhouse, quests, shop, news, settings,
plant selection, mini-game results) now goes through, so the window keeps redrawing while the
server answers.

### Server side

`network/server/`, started by `ServerApplication.main()` (default port 7070):

```
ServerApplication.start()
  → loads GameDataManager (server needs plant/zombie data too, for pricing/rewards)
  → opens a ServerSocket
  → starts MatchService's shared clock (one thread ticking every live match)
  → loop: accept() → spawn one daemon Thread per client → new ClientConnection(...).run()
```

- **`ClientConnection`** — one thread per client. Reads lines, decodes them, and hands each one
  to `RequestRouter.handle(...)`; a bad request is caught per-message so it can't kill the
  connection. On disconnect, tells the router so it can clean up (unbind the session, etc.).
- **`RequestRouter`** — the dispatcher. A handful of message types (register, login,
  token-login, password reset, ping) are handled before authentication; everything else requires
  `connection.isAuthenticated()` and falls into a `switch` over `MessageType`. It also implements
  `MatchService.Listener` — it's the only source of the `MATCH_STATE_UPDATE`/`MATCH_ENDED` pushes.
- **`SessionManager`** — one map, username → the `ClientConnection` currently bound to it. A
  second login attempt for an already-bound username is refused ("already logged in elsewhere"),
  not silently allowed.
- **`AuthenticationService`** — reuses `model.core.AuthService`'s validators (the same rules the
  offline game always used), hashes passwords the same way, issues a random session token for
  "stay logged in".
- **`ServerAccountStore` / `ServerAccount`** — the actual account database: one JSON file,
  `data/database/server-accounts.json` (sitting next to `Users.json`), rewritten in full on every
  change via `JsonSerializer`. There is no real database — this is the entire persistence layer
  for every account on the server.
- **`MatchmakingService`** — a plain queue for random matchmaking (first-in-line becomes your
  opponent) plus a UUID-keyed invite map for direct invites (an invite can only be accepted by the
  account it was actually sent to, closing a hijack-by-guessing-the-UUID bug).
- **`MatchService` / `NetworkMatch`** — owns every live networked match and one shared clock
  (a single scheduled thread ticking every match at a fixed interval, on purpose: "one loop for
  all matches rather than one thread each, so two players cannot end up on two slightly different
  clocks"). A move flows: client sends `GAME_ACTION` → `RequestRouter.gameAction` → the match's own
  authoritative `IZombieMatch` engine applies it → the resulting snapshot is pushed to *both*
  players as `MATCH_STATE_UPDATE`. There is no client-side simulation of a networked match — the
  client only ever draws whatever snapshot the server last sent it.
- **`LeaderboardService`** — not a separate store. Every request is computed live off every
  account's `gameData` document (adventure progress, mini-games cleared, quests done, best score),
  so it always reflects whatever `PROFILE_UPDATE` last wrote, with nothing cached.

## 3. Starting and running a (local) match

This is the same whether the terminal or the graphical client triggers it — neither talks to the
server for this part; a local adventure/mini-game/bonus match is entirely client-side.

```
MatchSetup (singleton)          — holds what the player picked: chapter/level or mini-game,
                                   selected deck, difficulty — between the plant-selection screen
                                   and the moment the match actually starts
       │
       ▼
MatchLauncher.launch()          — (or BonusGameLauncher / MiniGameLauncher — same skeleton,
  → picks a Season by stage       different rule set / zombie pool / return menu)
  → new GameManager(...)
  → gameManager.initializeLevel(rows, cols, waves)
  → attaches special-stage rules (boss / conveyor / etc, from the doc's 7 stage types)
  → gameManager.startGame()
       │
       ▼
GameSession.start(gameManager, returnMenu)   — the one static "current match" slot every
                                                screen/controller reads from
       │
       ▼
GamePlayController (terminal)  or  GameplayScreen (graphical)
  drives GameManager.advanceTime() once per tick
```

`GameManager.advanceTime()` is the actual simulation step: it advances the board, the current
wave, checks win/lose conditions, and drains a handful of "what happened this tick" queues off
`Board` (kills, plant losses, loot drops, notices) that both front ends read from — the terminal
prints them, the graphical client turns them into toasts/effects (see part 4).

When the match ends, `MatchCompletion.apply(match)` runs (called from both front ends): applies
score/rewards to the signed-in `User`, advances adventure progress (only if the cleared level
matches the account's current cursor, so replaying an old level can't double-reward), and — for a
bonus run — submits the score to the server via `ClientSession`/`SCORE_SUBMISSION`. It always ends
by calling `UserManager.updateCurrentUserGameState()`, which is what actually pushes the updated
save back to the server.

## 4. The graphical client's own flow

`view/gdx/core/PvzGdxGame` is the LibGDX `Game`: it owns the one shared `RenderContext` (batch,
camera, viewport), the one shared `GameAssets`, and the one shared `UiSkinProvider`, and swaps
`Screen`s via `switchScreen()` (which disposes the outgoing screen — plain `setScreen()` does not).

```
PvzGdxGame.create()
  → LoadingScreen        (App.initData(), see part 1)
  → MainMenuScreen        every other screen (~28 of them) hangs off this one, all navigable
                           to and from each other
```

Every menu screen extends `BaseScreen` → `MenuScreen` (login, sign-up, profile, shop, greenhouse,
news, quests, leaderboard, settings, collection, adventure map, plant selection, ...). Gameplay
screens (`GameplayScreen`, and the mini-game screens under `ArcadeBoardScreen`) extend `BaseScreen`
directly, since they draw a live board rather than a Scene2D form.

`GameplayScreen`'s per-frame draw order (this is the actual z-order of everything on the lawn):

```
LawnRenderer        → background, tiles, tile effects (water/ice/tombstones), lawnmowers
StageRuleRenderer    → whatever the level's special stage type needs drawn (conveyor belt, etc.)
EntityRenderer       → plants → zombies → projectiles → suns → loot pickups → impact/death/
                       spark effects, row by row (back row first, so nearer rows overlap it)
CursorRenderer       → what's under the player's cursor (held seed, shovel, etc.)
```

`EntityRenderer` also owns `HitEffects`, the short-lived-visual-feedback tracker: it *observes*
the board every frame (health that dropped, a projectile that vanished, a zombie that just died, a
new plant, a sun that was collected, an armour piece that broke) and turns each transition into a
fading effect — a hit flash, an impact burst, a death puff, a "planted" spark, a screen shake — all
without the model ever knowing any of this exists. Nothing about it can feed back into gameplay.

Plant and zombie art comes in two forms per entity: a rig (`view/gdx/animation/AnimationLibrary` +
`EntityAnimation`, baked from a PopCap `.PAM` file) if one exists, or a static portrait
(`PlantArt`/`ZombieArt`) if not. Which *clip* of a rig plays (idle vs. attack vs. eat vs. walk vs.
a dozen zombie-specific ones like Newspaper's pre-/post-enrage variants or the Jester's spin) is
decided per frame in `EntityRenderer.plantClip`/`zombieClip`, by asking the rig
(`EntityAnimation.pickClip`) for the first candidate name it actually has.

## 5. Where things are persisted

| File | What | Who's authoritative |
|---|---|---|
| `data/database/server-accounts.json` | Every account: password hash, profile, coins/diamonds, best score, and the full `gameData` save | The server. This is the real database. |
| `data/database/Users.json` | A local mirror of accounts this machine has signed into | Nobody — written on every successful account call, never read back for auth |
| `data/database/session.json` | The current "stay logged in" username + token | This machine only |
| `plants.json` / `Zombies.json` / `Quests.json` | Static game-design data (costs, stats, abilities) | Read-only, loaded once by `GameDataManager` at startup, same file for terminal/graphical/server |

## Two menu systems (a heads-up, not a redesign)

CLAUDE.md documents this in detail for the terminal side, but it's worth restating here since it
affects where to add things: the terminal front end still has *two* competing input-loop
mechanisms layered on top of each other (`Menu`/`Router`), and the graphical client is a *third*,
completely separate navigation system (`PvzGdxGame`/`Screen`) that doesn't touch either. A new
account/game feature generally needs wiring in twice — once for the terminal controller, once for
a graphical screen — both ultimately calling the same `model`/`UserManager`/`ClientSession` code.
