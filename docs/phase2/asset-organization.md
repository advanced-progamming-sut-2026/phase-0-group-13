# Phase 2 — Asset organization

Reference notes for the resources added in Phase 2. No graphics code exists yet; this
describes where things live and what shape they are in, so the renderer work can start from
a known map.

## Two tiers

| Tier | Path | Purpose | Shipped? |
|---|---|---|---|
| Source library | `resources/` | Raw upstream data, kept verbatim. Read by tools and humans. | No — gitignored |
 | Runtime root | `assets/` | Curated, game-facing assets. On the classpath. | Yes |

`assets/` is registered as a resources source dir in `build.gradle`, so anything under it is
reachable at runtime via `Gdx.files.internal("textures/plants/...")`. `resources/` is not on the
classpath and must never be put there — it is ~840 MB.

## What the archive actually contains

`resources/_source/pvz-assets.zip` (362 MB) extracts to 3 775 entries with no audio at all:

| Kind | Count | Where | Notes |
|---|---|---|---|
| Texture atlases | 780 `.PNG` | `resources/raw/pvz2/ATLASES/` | 1024×1024 sheets. The only real pixels. |
| Animations | 1 458 `.PAM` | `resources/raw/pvz2/IMAGES/768/…` | PopCap animation format, **not** images. |
| Master manifest | `RESOURCES.json` | `resources/raw/pvz2/` | 36 MB. Logical name → atlas + rect. |
| Animation index | `animations.json` | `resources/raw/pvz2/` | name, path, canvas size, clip durations. |
| UI skin | 9 files | `resources/skin/` | libGDX Scene2D skin: atlas + json + 6 TTF. |
| Sounds | 0 | — | Must be sourced separately. |

### Why the upstream tree is not re-foldered

Individual sprites are not files. A plant's frames live inside a shared atlas and are addressed
by rectangle:

```json
{
  "type": "Image",
  "id": "IMAGE_SPACE_SPIRAL_RADIAL_SPACE_GRADIENT",
  "path": "images\\1536\\initial\\space_spiral\\radial_space_gradient",
  "parent": "ATLASIMAGE_ATLAS_ALWAYSLOADED_1536_00",
  "ax": 199, "ay": 371, "aw": 164, "ah": 164
}
```

One atlas holds sprites from several categories, so `ALWAYSLOADED_768_00.PNG` cannot be filed
under `textures/plants/`. Separately, the `768/FULL/PLANT/PEASHOOTER/…` paths are the literal
keys used by `RESOURCES.json`, `animations.json` and `resources/metadata/pam-index.txt` —
rearranging that tree breaks all three lookups at once.

Category separation therefore happens on the way **out** of the library (extract → name → drop
into `assets/textures/plants/`), not inside it.

## Layout

```
resources/                          source library — gitignored, never on the classpath
├── raw/pvz2/                       upstream extraction, paths kept verbatim
│   ├── ATLASES/                    780 atlas PNGs
│   ├── IMAGES/768/{DEV,FULL,INITIAL}/<CATEGORY>/<NAME>/<NAME>.PAM
│   ├── RESOURCES.json              master manifest
│   └── animations.json             animation index
├── skin/                           pvz2_skin.{atlas,json,png} + 6 TTF — keep co-located
├── metadata/                       atlas-index.txt (780), pam-index.txt (1 458)
├── sounds/                         empty; the archive ships no audio
└── _source/pvz-assets.zip          original archive

assets/                             runtime root — on the classpath
├── textures/{plants,zombies,environment,lawn,ui,effects}/
├── animations/{plants,zombies,effects}/
├── sounds/{sfx,music}/
├── fonts/
└── metadata/

tools/pvz-libs/                     libPVZ, pvz-asset-browser, pvz-skin (zipped, unextracted)
docs/phase2/pvz-skin-field-guide.html   skin field guide
```

## Java infrastructure

Three new types in `view.gdx.assets`, wired to nothing:

- `AssetPaths` — classpath prefixes for the five runtime roots.
- `AssetCategory` — enum of categories and their folder names.
- `AssetRegistry` — the loader contract. No implementation by design.

`GameAssets`, `PvzGdxGame`, `BaseScreen` and `DesktopLauncher` were left untouched.

## Open items for the renderer phase

1. **Atlas conversion.** `RESOURCES.json` carries the same information as a libGDX `.atlas`
   file. A one-off converter reading it and emitting `.atlas` sidecars next to each PNG would
   let `TextureAtlas` load them directly, with no custom parser at runtime.
2. **PAM decoding.** `.PAM` is a PopCap format libGDX cannot read. Either decode it (the
   `tools/pvz-libs/libPVZ-main.zip` sources are a starting point) or bake the clips listed in
   `animations.json` into flat frame sequences at build time.
3. ~~**Skin path collision.**~~ **Settled: `resources/skin/` wins, the jar is not used.** The
   published `pvz-skin` v0.1.0 is an older export — it has no `CheckBox` or `SelectBox` style and
   splits the atlas over two pages — so the repo's copy is the one to keep. `processResources`
   copies `resources/skin/` to the classpath path `skin/`, and `UiSkinProvider` loads
   `Gdx.files.classpath("skin/pvz2_skin.json")` from there. Nothing else claims that path and
   there is no third copy under `assets/`. The skin json needs FreeType and TenPatch to parse, so
   both are dependencies now, plus the FreeType natives as `runtimeOnly`.
   `UiSkinProvider.FreeTypeSkin` is the Skin Composer font-block recipe, the one thing that had to
   be taken from the library.
4. **Naming map.** Upstream ids (`FULL/PLANT/PEASHOOTER`) do not match `PlantType` /
   `ZombieType` constants. A lookup table belongs in `assets/metadata/`, not in model code.
5. **Selection.** 80 plants and 233 zombies are available upstream against a much smaller
   roster in `PlantType`/`ZombieType`. Only extract what the game actually uses.
6. **Audio.** No sounds shipped. `assets/sounds/` is a placeholder.
