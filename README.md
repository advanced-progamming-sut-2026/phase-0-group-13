# 🌻 Plants vs. Zombies 2 — Group 13

A Plants vs. Zombies 2 clone in plain Java: a full graphical game on LibGDX, a terminal build of
the same model, and a client/server stack for playing I-Zombie against another person.

Sharif University of Technology — Advanced Programming, Group 13.

| 🧑‍🎓 نام و نام خانوادگی | Name | 💳 شماره دانشجویی |
| :---: | :---: | :---: |
| **امیرحسین بازدار** | Amirhossein Bazdar | `404105575` |
| **آرش یوسف‌نژاد** | Arash Yousefnezhad | `404110248` |
| **ایلیا اصلاحی** | Iliya Eslahi | `404172292` |

<samp>Java 25 · Gradle 9.3 · LibGDX 1.12.1 · 319 source files · 403 tests</samp>

---

## Contents

- [Quick start](#quick-start)
- [Shipping it](#shipping-it) — building the standalone jars
- [What's in the game](#whats-in-the-game)
- [How it's built](#how-its-built)
- [Project layout](#project-layout)
- [Where your data lives](#where-your-data-lives)
- [Testing](#testing)
- [Things worth knowing](#things-worth-knowing)

---

## Quick start

Everything runs through Gradle — no manual `javac`, no IDE required.

```bash
./gradlew runGdx
```

That's the game. For multiplayer, start the server first in another terminal:

```bash
./gradlew runServer
```

It listens on **7070** by default; `-Pport=7071` moves it. The client connects on startup and
carries on happily offline if nothing answers — multiplayer is simply unavailable until a server
is there.

| Task | What it does |
| :--- | :--- |
| `./gradlew runGdx` | The graphical game (add `-Ppvz.debug=true` for the debug controls) |
| `./gradlew runServer` | The match/account server |
| `./gradlew runTerminal` | The Phase 1 stdin/stdout build, still driven by the same model |
| `./gradlew test` | The full JUnit suite |
| `./gradlew jars` | Both standalone jars — see below |

---

## Shipping it

```bash
./gradlew jars
```

| Jar | Size | Run it |
| :--- | ---: | :--- |
| `build/libs/pvz-game.jar` | 70 MB | `java -jar pvz-game.jar` |
| `build/libs/pvz-server.jar` | 2.2 MB | `java -jar pvz-server.jar [port]` |

**Both are self-contained.** Copy them anywhere — no project tree, no `assets/`, no
`resources/` folder beside them. Everything the game opens is packed inside: the 106 PAM rigs the
animation manifests name, every texture atlas, and the plant/zombie/quest templates.

The build works out which rigs to pack by reading the manifests, so `resources/raw` contributes
only the 23 MB that is actually loaded rather than all 511 MB of it.

On macOS the game re-launches its own JVM with `-XstartOnFirstThread` when that flag is missing,
because GLFW refuses to run on any thread but the process's first. You don't have to pass
anything; plain `java -jar` is enough on every platform.

> **Note** — saved games and accounts are *not* packed into the jars. They are personal data, so
> a jar you hand to someone else starts them with a clean profile.

---

## What's in the game

**The lawn.** 69 plants and 47 zombies, built from the real game's stat tables in
[`src/data/database/`](src/data/database). Plants are grouped by what they do rather than by class
hierarchy — Shooter, Lobber, Explosive, Homing, Melee, Modifier, Strike-through, Sun Producer and
Wall-nut — and each group is a behaviour strategy the factory picks at build time.

**Four chapters**, each with its own hazards, tile rules and zombie roster:

| Chapter | What makes it different |
| :--- | :--- |
| 🏜️ Ancient Egypt | Tombstones, and sandstorms that blow zombies deep into the lawn |
| 🏰 Dark Ages | Permanent night — no natural sun, gravestones spawn zombies |
| ❄️ Frostbite Caves | Ice trails that freeze plants; only fire thaws them |
| 🌊 Big Wave Beach | Tides flood the right-hand columns; land zombies can't cross water |

**Five mini-games.** Vasebreaker, Wall-nut Bowling, I-Zombie and Beghouled each run on their own
compact engine; Zombotany is played on an ordinary lawn against zombies wearing plant heads.

**Everything around the lawn.** Accounts with password recovery, a shop and inventory, a
greenhouse where plants grow between matches, quests, an almanac, a news feed, a leaderboard,
difficulty settings and an adventure map that unlocks as you go — 25 screens in all.

**Multiplayer.** Two players, one I-Zombie match: one places zombies, the other defends. The
server owns the simulation and pushes a fresh snapshot ten times a second.

---

## How it's built

### The tick

The whole simulation runs on a fixed clock at **10 ticks per second**. `GameManager.advanceTime()`
is called once per tick and updates the board, the wave and the win conditions. The renderer runs
free at whatever the display gives it and interpolates between the last tick and the current one,
so movement is smooth without the model ever knowing what a frame is.

Nothing in the view may change simulation state. Animations are *scheduled around* model events —
a shooter's attack clip is timed so its release frame lands exactly on the tick its projectile is
created, rather than the projectile being moved to suit the animation.

### Factories and strategies

```
PlantTemplate (JSON)  ──▶  PlantFactory  ──▶  Plant + PlantAction
ZombieTemplate (JSON) ──▶  ZombieFactory ──▶  Zombie + ZombieAction
```

`PlantAction` and `ZombieAction` are the strategy interfaces — `ShootForwardAction`, `LobAction`,
`ExplodeAction`, `GargantuarAction` and so on. Adding a new category means adding a case to the
factory's `determineBehavior` switch and one new `*Action` class; nothing else has to change.
Plant Food is a second behaviour wrapped around the first by the same mechanism.

### Rendering

Sprites are not sprite sheets — they are **PAM rigs**, the original game's skeletal animation
format, parsed at load time and posed per frame against a `TextureAtlas`. A manifest in
[`assets/animations/`](assets/animations) ties an entity to its rig, its atlas and its named clips.
That is why a zombie can lose an arm, an armour piece can come off independently of the body, and
a Pianist can push a piano that is a separate rig riding the same canvas.

### Network

A line-oriented TCP protocol carrying JSON — `NetworkMessage` with a `MessageType` and a typed
payload. The server holds accounts, sessions, matchmaking and the leaderboard; live matches are
ticked on a single clock thread while each player's moves arrive on their own connection thread.

---

## Project layout

```
src/
  model/            the game itself — no rendering, no I/O
    core/           App, Router, GameManager, AuthService
    game/           Board, plants, zombies, projectiles, minigames, shop, quests
    environment/    seasons, stages, greenhouse, attack patterns
    account/        users, profiles, progress, inventory
    enums/          types + the regex command enums for the terminal build
  view/
    gdx/            the LibGDX client — screens, renderers, animation, audio, UI
    *.java          the terminal build's menus
  controller/       input handling for the terminal build
  data/             JSON persistence, repositories, GameDataManager
  network/
    client/         ClientSession, NetworkClient
    server/         ServerApplication, RequestRouter, MatchService, …
    protocol/       NetworkMessage, MessageType, Payloads
test/               403 tests, mirroring the src package layout
assets/             textures, atlases, animation manifests, audio, skin
resources/raw/      the PAM rigs
docs/               architecture notes and asset documentation
```

---

## Where your data lives

Game templates (`plants.json`, `Zombies.json`, `Quests.json`) are read-only. Saves, accounts and
the signed-in session are written back.

**Running from the project** — everything stays in `src/data/database/`, so you can edit a
template and see the change on the next run without rebuilding.

**Running from a jar** — the templates are unpacked once into `~/.pvz/database/`, and saves are
written there too.

> **Warning** — from source, the working directory must be the project root. `DataPath` registers
> relative paths, and a run from the wrong directory loads *no* templates and fails quietly rather
> than loudly. The Gradle tasks always get this right.

---

## Testing

```bash
./gradlew test
```

**403 tests** across 62 classes, roughly 8,400 lines of them. They live in `test/` rather than
`src/` — the main source set's root *is* `src`, so tests inside it would be compiled into the game.

The suite covers the model directly (board rules, plant and zombie behaviour, seasons, waves,
economy, minigame engines, auth) and the parts of the view that can be reasoned about without a
window — animation scheduling, rig coverage, art lookups. Concurrency is tested where it matters:
`MatchThreadSafetyTest` drives the server's real threading pattern across 40 matches.

---

## Things worth knowing

- **Two menu systems coexist.** The `Menu` enum loop and the `Router` singleton loop are both
  live, a historical artifact from earlier phases. Trace both when following control flow through
  the terminal build. `docs/architecture-overview.md` has the detail.
- **`src/out/` and `src/src/` are not source.** Both are committed accidents — stale build output
  and a duplicated tree. The real root is `src/`.
- **Persian comments are deliberate**, not mojibake. Sources are UTF-8; the build sets the encoding
  explicitly so Windows default code pages don't break the build.
- **Style** is Google Java Format with a course Checkstyle config at
  [`sharif_checkstyle.xml`](sharif_checkstyle.xml) — 120 columns, 50-line methods.

---

<div align="center">

Built for Advanced Programming at Sharif University of Technology.

**امیرحسین بازدار · آرش یوسف‌نژاد · ایلیا اصلاحی**

</div>
