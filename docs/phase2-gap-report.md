# Phase 2 — Rubric Gap Report

Audit of the codebase against the official Phase 2 grading sheet
(`AP project 2026.xlsx`, sheet "phase 2"): **239 criteria, 7385 points**
(4045 mandatory + 2425 polish/زیبایی + 885 bonus/امتیازی).

Everything below was verified by reading the code, not assumed. Grading is on the **graphical
build only**, so a feature that exists in the model but is never drawn counts as missing.

---

## Part 0 — What was already fixed (done, compiling, tests pass)

These were genuine bugs (dead assets, wrong values, missing wiring), fixed in this pass:

| # | Item | Pts | What was wrong |
|---|---|---|---|
| CO | Radioactive sun colour | 35 | Drawn **green**; the doc explicitly says **purple**. One-line fix. |
| FS | Projectile fire/impact SFX | 35 | `Sfx.SHOOT` + `shoot.wav` existed but were **never played**. Wired shot + impact sounds. |
| Q | Chapter name on adventure cards | 40 | Cards showed only "3 / 10 levels" — no chapter name. Name now leads the caption. |
| AP | News title | 30 | `News` had no title field; screen showed a lowercase type tag. Added real titles + fallback for old saves. |
| DK | "Not enough sun" on card click | 10 | Clicking a dimmed seed card silently armed the cursor with no explanation. |
| GD/GE/GF/GG/GI | 5 zombie ability animations | 115 | All-Star kick, Turquoise drain, Tomb Raiser summon, Hunter throw, Octopus toss — every rig **ships the clip**, none were ever requested. |

Also fixed earlier in the session: growth-stage plant clips (Sun-shroom/Kiwibeast), Chomper/
Magnet-shroom/Squash/Fume-shroom action clips, the `pickClip` algorithm bug that pinned Caulipower
and Electric Blueberry permanently in their attack pose, Endurian/Garlic/Sweet Potato action ticks,
Newspaper/Barrel-Roller prop states, the Jester spin, Fisherman cast, Gargantuar throw pose,
Ra's heal aura, Parasol shield tint, Snorkel submerged fade, and the lawnmower render position.

---

## Part 1 — The big finding

**~600 points are sitting in unused animation clips.** The rigs already contain the artwork; the
renderer simply never asks for it. This is by far the cheapest scoring work left, and it is the
same one-line-per-case pattern already used for `plantClip()` / `zombieClip()`.

| Item | Pts | Status |
|---|---|---|
| **GB — plant-food animation** | **250** | Every one of the 57 plant rigs has `plantfood*` clips. **Zero are requested.** |
| **GL — zombie death animation** | **175** | All 37 rigs have `die`/`die2`. Never requested; dead zombies are skipped from drawing entirely. |
| **ET — heads fly off on death** | 100 | `particle_head` part exists per rig, never used. |
| **GK — Gargantuar smash** | 60 | Gargantuar sets `setEating(true)`, so it plays `eat`; `smash_left` is consumed by the imp throw. |
| **FX — defensive plant damage stages** | 35 | `wallnut`/`tallnut`/`garlic` have `damage`/`damage2`/`damage3`; never requested. |
| FZ — charge plants arming | 30 | `potatomine` has `plant`/`plant_idle`/`recover`; never requested. |
| FV — sun-production animation | 35 | Only Sun-shroom mapped; Sunflower/Twin/Primal fall back to idle. |
| HN/HT/HW/IB — Zomboss spawn/wind clips | 60 | Rigs have `zombie_portal_*`, `wind_1..4`, `glacier_column_*`, `spawn`. `bossClip()` never requests them; no `SUMMONING` pose exists. |
| HI — Dark Ages fire animation | 15 | Reuses generic attack; `fire_bomb` clips unused. |

---

## Part 2 — Missing / incomplete, by area

### Mandatory (4045 pts)

**Menus** — strong overall (66/66 criteria present). Remaining:
- `AL` (20) **Shop daily-item countdown** — only a static "TODAY ONLY" ribbon; `Shop` exposes no expiry time.
- `V`/`W` (100) **Collection idle animations** — clicking a plant/zombie shows a static seed-packet sprite, not the idle rig. Stats are complete.
- `AB` (15) **Greenhouse pot idle animation** — static sprite.
- `BJ` (40) **Level-start quest list** — popup shows waves + objective text, but not the level's quests; rules are named by raw Java class name (e.g. `LoveYourPlantsRule`).
- `N` (10) — "Highest My-Point" shows cumulative `meowPoints`, not the max.

**In-game info / board (1525 pts)**
- `CM` (25) **Sun falling is not continuous** — `Sun.update()` only decrements a counter; `x`/`y` never change, so sky suns teleport.
- `CI` (125) **Projectiles not continuous** — position updates once per 10 Hz tick with no interpolation between frames.
- `CP` (25) **Plant-food-carrying zombies unmarked** in the graphical build (`isShiny` is drawn only by the terminal renderer).
- `CB` (250) — 59/69 plants have idle rigs; 10 (mints, Cat-tail) fall back to a static sprite.
- `BQ` (25) — wave meter steps per wave; not a continuous advance indicator.
- `BX` (60) — one NPC (Penny), one line; doc wants a multi-line exchange.
- `BV`/`BW` (50) — necromancy and low-tide announcements are generic per-wave suffixes, not real events.
- `CT` (15) — a spent lawnmower stays drawn at 0.35 alpha; doc says it should disappear.
- `CU` (25) — planting ghost uses a static packet, not the idle rig.
- `DC` (20) — debug +sun/+plant-food buttons sit bottom-right, not beside the counters.

**Stage types / minigames / seasons (825 pts)**
- `EB` (25) **I,Zombie cursor animation** — missing entirely; `CursorRenderer` is only used by `GameplayScreen`.
- `EC` (25) — only the hovered cell highlights, not the whole row.
- `EI` (25) — frozen zombies get a blue tint but **no ice block** is drawn.
- `DL`/`DM` (55) — conveyor shows the whole pool but only the head is plantable, no cost shown, and a planted item stays greyed in the bar instead of being removed.
- `DP` (20) — timed-battle objectives show a timer only, not per-objective done/not-done state.
- `DZ` (30) — wall-nuts move in integer cell steps, not continuously.
- `EO` (15) — necromancy cells only marked where a grave already sits.

### Polish / زیبایی (2425 pts)
Beyond the clip items in Part 1:
- `ER` (100) **Shot timing** — the clip starts at `lastActionTick`, i.e. *after* the pea already spawned; there is no in-clip release frame.
- `FP` (50) **Projectile spawn position** — projectiles are created at the tile centre, not the plant's mouth.
- `EU` (75) — no arm-detach logic at all; `arm_outer_upper_bone` is force-hidden.
- `EV` (125) — armour vanishes with a spark; no physical piece falls.
- `FG`/`FH`/`FI` (105) — conveyor belt is a static table: no motion, no stacking, no animated background.
- `FL` (25) — plant food is auto-credited; never lands on the lawn to be collected manually.
- `FF` (25) — no plant-food glow behind the plant.
- `FY` (50) — no mint rigs exist at all.
- `GN` (50) — zombies slide: `x` steps per tick while the walk clip runs at its own fps, uncoupled.
- `FB` (15) — octopus has no damage flash.
- `FC` (30) — graves only darken; no damaged-grave stages.
- `EP` (15) — explosive plants play `attack` during the fuse, not at detonation.
- `FK` (30) — ash drawn for every death, not only explosion kills.

### Bonus / امتیازی (885 pts)
- `GO` (50) **Save game — not implemented.** `Games.json` is a 0-byte file; "Save & Exit" only persists the account and abandons the match.
- `GS` (30) — Beghouled swaps/collapses mutate the grid instantly; no tweening.
- `GW` (80) — score patterns fire but notify only via `System.out.println`; no in-game popup.
- `HK`/`HQ` (40) — Egypt missile and mammoth ice chunk apply instantly; no falling projectile.
- `HX`/`HY` (45) — no shark entity; turbine drags zombies but destroys plants instantly.
- `HL` (20) — Egypt charge kills plants but never other zombies.
- `HS` (25) — mammoth freezes existing zombies instead of spawning frozen ones.

---

## Part 3 — Work split for 3 people

Balanced by effort, and grouped so the three people touch **different files** and won't conflict.

### Person A — Animation wiring (~700 pts, highest value/effort ratio)
Almost entirely `EntityRenderer.plantClip()` / `zombieClip()` / `bossClip()` plus small model getters.
The pattern is already established — copy how `ACTION_CLIP_NAMES` and `abilityClip()` work.

1. **GB — plant-food animation (250).** Add an "is plant food active" accessor to `Plant`, request `plantfood*` clips while active. Check ≥10 plants.
2. **GL — zombie death animation (175).** Keep a dying zombie drawn for its `die` clip duration instead of skipping it; `HitEffects.observeZombieState` already detects the transition.
3. **ET (100)** heads fly off + **GK (60)** Gargantuar 2-stage smash (stop it setting `setEating(true)`).
4. **FX (35)** wall-nut/tall-nut/garlic damage stages, **FZ (30)** potato-mine arming, **FV (35)** sun-production clips.
5. **Zomboss clips (75):** add a `SUMMONING` pose to `ZombossAction`, request `zombie_portal_*` / `wind_*` / `glacier_column_*` / `spawn`, plus `fire_bomb` for HI.

*Files:* `view/gdx/render/EntityRenderer.java`, `model/game/plant/Plant.java`, `model/game/zombie/behavior/ZombossAction.java`, `GargantuarAction.java`

### Person B — Motion & continuity (~450 pts)
Everything the rubric calls "پیوسته" (continuous). Mostly interpolation between ticks.

1. **CI (125) + ER (100) + FP (50)** — projectile interpolation between ticks, release synced to the shoot clip, spawn from the plant's mouth (needs a per-plant muzzle offset).
2. **GN (50)** — stop zombies sliding: couple walk-clip phase to actual movement.
3. **CM (25)** — make sky suns actually fall (animate `Sun.y`).
4. **FG/FH/FI (105)** — conveyor belt: continuous card motion, stacking, animated background.
5. **DZ (30)** — continuous wall-nut rolling.

*Files:* `model/game/Projectile.java`, `model/game/Sun.java`, `model/game/zombie/Zombie.java`, `view/gdx/ui/ConveyorBar.java`, `model/game/minigame/ConveyorRule.java`, `arcade/WallnutBowlingEngine.java`

### Person C — Screens, HUD & missing features (~500 pts)
UI work, no renderer internals — safest to parallelise.

1. **GO (50)** — save/resume a match. `Games.json` and the `DataPath` "games" key already exist, unused.
2. **V/W/AB (115)** — idle animations in Collection and Greenhouse (needs a small Scene2D actor that draws an `EntityAnimation`; Person A's work does not overlap).
3. **EB/EC (50)** — I,Zombie cursor animation + row highlight; reuse `CursorRenderer` in `ArcadeBoardScreen`.
4. **BJ (40)** — level-start quest list, and give the stage rules human-readable names.
5. **DP (20) + DQ + DL/DM (55)** — objective status per goal; conveyor card removal + cost.
6. **BX (60)** — multi-line NPC dialogue at level start.
7. **GW (80)** — on-screen score-pattern notifications (currently `println` only).
8. **AL (20)** — shop daily-item countdown; **CP (25)** mark plant-food zombies; **CT (15)** hide spent mowers; **DC (20)** move debug buttons beside the counters.

*Files:* `view/gdx/screens/*`, `view/gdx/ui/HudStage.java`, `Dialogue.java`, `model/game/ScoreManager.java`, `model/game/shop/Shop.java`

---

## Suggested order

If time is short, do these first — best points per hour:
1. **GB (250)** and **GL (175)** — pure wiring, the artwork already exists.
2. **ET (100)**, **GK (60)**, **FX/FZ/FV (100)** — same pattern.
3. **GO (50)** — save game is the only completely absent bonus feature.
4. Everything under Person B, which is real engineering and will take longest.
