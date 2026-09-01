# Phase 2 — Rubric Gap Report (re-audit, 1 Sep 2026)

Second pass over the codebase against the Phase 2 grading sheet
(`AP project 2026.xlsx`, sheet "phase 2": 239 criteria, 7385 points —
4045 mandatory + 2425 زیبایی + 885 امتیازی).

**Since the first audit, the three-way split has largely landed.** Of ~35 items that report
listed as open, **23 are now done**. This document is the remaining list, re-verified against
the code on 1 Sep 2026 — not carried over on trust.

Build state at time of writing: `compileJava` clean, **279 tests, 0 failures, 0 errors**.

---

## Closed since the first audit

Verified present in the code, item by item:

**Person A — animation wiring (the ~600-point block, essentially all of it):**
`plantfood*` clips wired for every plant that has one (`EntityRenderer.PLANT_FOOD_CLIPS`, and
`WiredClipCoverageTest` now fails the build if a rig loses one) · zombie death clips held on
screen for their own duration (`DyingZombie`) · Gargantuar `smash_left` split from the imp
throw · wall-nut/tall-nut/garlic `damage`/`damage2`/`damage3` stages · Potato Mine
`plant`/`plant_idle` arming pose · Zomboss `SUMMONING` pose with `zombie_portal_*` /
`glacier_column_*` / `wind_*` / `spawn` / `fire_bomb`.

**Person B — motion & continuity:**
projectile interpolation between ticks (`Projectile.previousX/previousY` + `tickAlpha`) ·
shot release synced to the clip's release frame (`shotColumn`) · muzzle offset taken from the
rig's own head box (`MUZZLE_HEAD_REACH`) · suns actually fall (`Sun.fallingTicks` +
`previousY`) · conveyor belt runs continuously with treads and per-card cost
(`ConveyorBar.act`) · wall-nut bowling interpolates between cells
(`previousLane`/`previousColumn`).

**Person C — screens, HUD & features:**
save/resume a match (`MatchSave` + `MatchSaveManager`, and four bugs found and fixed in it —
see below) · idle rigs in Collection and Greenhouse (`RigActor`) · I,Zombie placement ghost
and whole-row highlight · level-start quest briefing · shop daily-item countdown ·
plant-food-carrying zombies marked on the lawn · spent lawnmowers removed · debug cheats moved
beside their counters · on-screen score-pattern notices (`ScoreManager.pendingNotices`) ·
grave wear stages · necromancy discs on graves.

**Bugs found and fixed in the debug pass** (committed as `d26324c`): resume silently discarded
the whole lawn (restore went through the *placement* path, so it re-charged sun, re-checked the
stage's plant rules and re-fired quest counters) · saving after the last wave resumed as a free
win · `clear()` had no ownership check, so one account starting a level deleted another's save ·
lawnmowers were refunded on resume · eating/frozen zombies vibrated at 10 Hz because
`previousX` went stale.

---

## What is actually left

### Blocked — missing source art, not a wiring gap

| Item | Pts | Finding |
|---|---|---|
| **FY — mint rigs** | 50 | The raw PvZ2 library has **no lawn rig for any mint**. `resources/raw/pvz2` carries only `EMPOWERMINTS` (icons) and `PRIZE_PINATA_*MINT` (the piñata reveal effect). There is nothing to bake. |
| **CB — plant idle animations** | (part of 250) | 59/69 plants have idle rigs. The 10 without are **Cat-tail + the 9 mints** — and Cat-tail has no raw entry at all. The rest of this criterion is earned. |

Options, if these points matter: re-purpose the `PRIZE_PINATA_*MINT` effect rig as a stand-in
idle, or accept the static portrait fallback that is already in place. Neither is free work.

### Mandatory (اجباری)

| Item | Pts | Finding |
|---|---|---|
| **EI — frozen zombies** | 25 | Frozen zombies get `frozenTint` (a blue wash) and nothing else. No ice block is drawn anywhere — there is no ice region in the HUD atlas lookup at all. |
| **EO — necromancy cells** | 15 | The necromancy disc is drawn on graves that already exist (`LawnRenderer:306`). Cells that *will* raise a zombie are not marked ahead of time. |

### Polish (زیبایی)

| Item | Pts | Finding |
|---|---|---|
| **GN — zombies must not slide** | 50 | `AnimationStates.advance(entity, clip, delta)` runs the walk clip on **wall-clock delta**, while `x` steps once per 10 Hz tick. The two are still uncoupled; the interpolation added for CI smoothed the *position*, not the stride. |
| **FL — collect plant food by hand** | 25 | `Board` credits plant food straight to `GameState.addPlantFood()`. It never lands on the lawn to be picked up. |
| **FF — plant-food effect** | 25 | Partial. The `plantfood*` clip plays (that was Person A's GB work), but there is no glow behind the plant. |
| **EP — explosive timing** | 15 | Partial. The armed-mine pose landed, but a timed explosive still plays `attack` during its fuse rather than at detonation. |
| **FB — octopus damage** | 15 | `drawOctopusHold` draws a static region; the octopus has no damage flash. |

### Bonus (امتیازی)

Nothing outstanding — see Person C's section below.


---

### Two things this re-audit got wrong

Both were caught by tests, not by reading:

- **DP (20) was already done.** `GameplayScreen.objectiveText()` has rendered `[DONE]` / `[    ]`
  per objective for `TimedWarRule` all along. The re-audit only grepped `HudStage` for the label
  and never looked at the string being handed to it.
- **HS (25) was never a gap.** "زامبی‌های یخ‌زده" under the ice-caves boss means the zombies the
  player is *already* fighting get frozen, which the deep freeze already did. Making the mammoth
  spawn frozen zombies was implemented, and then reverted: `ZombossActionTest` and
  `ZombossChapterAttacksTest` both state that the doc gives the mammoth **no zombie spawning**.
  The existing tests were right and the re-audit's reading was wrong.

### Done since this re-audit — Person A's share (~330 pts)

`ET` heads (100) · `EU` arms (75) · `EV` armour pieces (125) · `FK` ash gating (30).

One system, in `EntityAnimation.drawLoosePart` + `DetachedParts` + `BodyParts`: a named part is
lifted out of the frame that poses it best and drawn on its own, spinning and falling under
gravity, while the body it left is hidden on the corpse so there is never a second head. Armour
throws the damage state it broke in. Ash now needs `Zombie.wasKilledByBlast()`, so a zombie shot
to pieces leaves dust and only an explosion leaves ash.

Verified against a real GL context, not just compiled: all 37 rigs load, **28 heads, 30 arms and
24 armour pieces actually draw**. `DetachablePartCoverageTest` pins the part names, since a name
that stops matching throws no piece and reports no error.

### Done since this re-audit — Person C's share (~185 pts)

`HK` Egypt missile (25) · `HY` turbine (30) · `HQ` mammoth boulder (15) · `HL` charge (20) ·
`HX` sharks (15) · `GS` Beghouled settle (30) · `N` highest My-Point (10).

- **A boss attack is now a thing on the lawn, not a `printf`.** New `BossHazard`: Egypt's missile
  and the mammoth's boulder are launched, fall onto the tile they are aimed at over ~1.4s behind a
  closing target ring, and do their damage on arrival; the beach boss's sharks swim up their rows
  and eat the first plant they reach. The board advances them without knowing what any of them are.
- **`HY`** — the turbine used to flatten both rows on the tick it switched on, so nothing was ever
  seen being pulled in. It now takes the row apart from the boss's end inwards while the suction
  runs, and drags its own zombies in bodily.
- **`HL`** — the charge used `setX`, which drags `previousX` along with it and left the renderer
  nothing to tween: a fast forward charge was drawn as ten 0.12-column jumps a second. New
  `Zombie.moveTo` keeps last tick's place. The charge now also tramples the boss's own zombies.
- **`GS`** — `GridMotion` compares two pictures of the Beghouled grid, works out how far each tile
  fell, and the board is drawn settling instead of snapping between frames.
- **`N`** — "Highest My-Point" showed the running total, so it only ever went up. `User` now
  records the best single result; the total is shown on its own line beside it.

Verified with 14 new tests, plus the shark art confirmed drawing against a real GL context.

## Totals

| Bucket | Points still open |
|---|---|
| Mandatory | ~40 — `EI` ice blocks (25), `EO` necromancy cells (15) |
| Polish (زیبایی) | ~130 — all of it Person B's lane |
| Bonus (امتیازی) | 0 |
| Blocked on art | ~50 + part of CB |

Everything still open is Person B's share plus the two small mandatory items, `EI` and `EO`.

---

## Suggested order

1. ~~**Body parts — ET + EU + EV (300).**~~ Done, see above.
2. **GN (50)** — drive the walk clip's phase from distance travelled instead of wall-clock
   delta. Localised to `EntityRenderer` + `AnimationStates`.
3. **The two mandatory leftovers (40)** — `EI` and `EO`. Small and independent.
4. **`FL` (25), `FF` (25), `EP` (15), `FB` (15)** — small polish, one file each.
