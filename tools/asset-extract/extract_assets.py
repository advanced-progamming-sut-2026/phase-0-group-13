#!/usr/bin/env python3
"""Phase 2 asset extraction: raw PvZ2 library -> runtime assets/ tree.

Closes open item 1 in docs/phase2/asset-organization.md for the roster the project
actually has. Reads ``resources/raw/pvz2/RESOURCES.json`` (which carries the same
information a libGDX ``.atlas`` file does: page, x, y, w, h) and emits, per selected
entity, a ``.atlas`` sidecar next to a copy of its atlas page.

Design notes
------------
* Only the 768 resolution is touched. ``resources/raw/pvz2/ATLASES`` holds 768 pages
  only; RESOURCES.json also describes 1536 pages that are not on disk.
* Upstream art is filed one folder per entity and, with two exceptions, one atlas page
  per entity, so category separation is a copy rather than a repack. The two entities
  that span two pages get a two-page ``.atlas``, which libGDX supports natively.
* Region names are the upstream path leaf, unchanged. Nothing is renamed.
* Selection is by exact match on a normalised name. Anything that does not match
  exactly is written to the unresolved list instead of being guessed at.
* Deterministic: every iteration is over a sorted collection, so a re-run byte-for-byte
  reproduces the same output.

Usage (from the repo root)::

    python tools/asset-extract/extract_assets.py          # write assets/
    python tools/asset-extract/extract_assets.py --dry-run
"""

from __future__ import annotations

import argparse
import collections
import json
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RESOURCES_JSON = os.path.join(ROOT, "resources", "raw", "pvz2", "RESOURCES.json")
ATLAS_DIR = os.path.join(ROOT, "resources", "raw", "pvz2", "ATLASES")
PLANTS_JSON = os.path.join(ROOT, "src", "data", "database", "plants.json")
ZOMBIES_JSON = os.path.join(ROOT, "src", "data", "database", "Zombies.json")
ASSETS = os.path.join(ROOT, "assets")

RESOLUTION = "768"
NEWLINE = chr(10)

# The four Season subclasses under src/model/environment map onto these upstream world
# tokens. This is the project's own mapping, not an inference about the art: the class
# name states the world and the folder token is matched exactly.
SEASON_WORLDS = {
    "AncientEgyptSeason": "egypt",
    "FrostbiteCavesSeason": "iceage",
    "BigWaveBeachSeason": "beach",
    "DarkAgesSeason": "dark",
}


def norm(text: str) -> str:
    """Lowercase, strip everything that is not a letter or digit."""
    return re.sub(r"[^a-z0-9]", "", (text or "").lower())


# --------------------------------------------------------------------------- loading


def load_resources():
    """Returns (sprite regions at RESOLUTION, atlas page records by id)."""
    with open(RESOURCES_JSON, encoding="utf-8") as handle:
        data = json.load(handle)

    pages, sprites = {}, []
    for group in data["groups"]:
        for res in group.get("resources") or []:
            if res.get("type") != "Image":
                continue
            if res.get("atlas"):
                pages[res["id"]] = res
            elif res.get("parent"):
                sprites.append(res)

    sprites = [s for s in sprites if s["path"].split("\\")[1] == RESOLUTION]
    return sprites, pages


def index_folders(sprites):
    """category -> folder -> {'pages': set, 'regions': [sprite, ...]}"""
    tree = collections.defaultdict(lambda: collections.defaultdict(
        lambda: {"pages": set(), "regions": []}))
    for sprite in sprites:
        parts = sprite["path"].split("\\")
        if len(parts) <= 4:
            continue
        entry = tree[parts[3]][parts[4]]
        entry["pages"].add(sprite["parent"])
        entry["regions"].append(sprite)
    return tree


# --------------------------------------------------------------------------- matching


def build_lookup(folders, strip_prefix=None):
    """normalised folder name -> folder name, first writer wins (sorted, so stable)."""
    lookup = {}
    for folder in sorted(folders):
        key = norm(folder)
        lookup.setdefault(key, folder)
        if strip_prefix and key.startswith(strip_prefix):
            lookup.setdefault(key[len(strip_prefix):], folder)
    return lookup


def match_plants(plant_folders):
    with open(PLANTS_JSON, encoding="utf-8") as handle:
        plants = json.load(handle)
    lookup = build_lookup(plant_folders)

    resolved, unresolved = {}, []
    for plant in plants:
        name = plant["Name"]
        folder = lookup.get(norm(name))
        if folder:
            resolved[name] = folder
        else:
            unresolved.append({"entity": name, "roster": "plants.json",
                               "reason": "no 768 art folder whose name normalises to "
                                         + norm(name)})
    return resolved, unresolved


def _zombie_candidates(alias, objclass):
    """Name forms to try, most specific first. Alias beats objclass."""
    out = []
    for raw in (alias, objclass):
        if not raw:
            continue
        key = norm(raw)
        for suffix in ("propertysheet", "props", "default"):
            if key.endswith(suffix):
                key = key[: -len(suffix)]
        out.append(key)
        if key.startswith("zombie"):
            out.append(key[len("zombie"):])
    return out


def match_zombies(zombie_folders):
    with open(ZOMBIES_JSON, encoding="utf-8") as handle:
        entries = json.load(handle)
    lookup = build_lookup(zombie_folders, strip_prefix="zombie")

    resolved, unresolved = {}, []
    for entry in entries:
        objclass = entry.get("objclass", "")
        if not objclass.startswith("Zombie") or not entry.get("aliases"):
            continue
        alias = entry["aliases"][0]
        folder = next((lookup[c] for c in _zombie_candidates(alias, objclass)
                       if c in lookup), None)
        if folder:
            resolved[alias] = folder
        else:
            unresolved.append({"entity": alias, "roster": "Zombies.json",
                               "reason": "no 768 art folder matches the alias or the "
                                         "objclass " + objclass})
    return resolved, unresolved


def match_worlds(folders, prefix):
    """Season -> folder, for the mower and background trees."""
    lookup = build_lookup(folders, strip_prefix=prefix)
    resolved, unresolved = {}, []
    for season, world in sorted(SEASON_WORLDS.items()):
        folder = lookup.get(world)
        if folder:
            resolved[season] = folder
        else:
            unresolved.append({"entity": season, "roster": "src/model/environment",
                               "reason": "no folder matches world token " + world})
    return resolved, unresolved


# --------------------------------------------------------------------------- emitting


def atlas_text(pages, regions, page_files):
    """libGDX atlas format. One block per page, regions sorted by name."""
    by_page = collections.defaultdict(list)
    for region in regions:
        by_page[region["parent"]].append(region)

    chunks = []
    for page_id in sorted(pages):
        record = page_files[page_id]
        lines = [record["file"],
                 "size: {},{}".format(record["width"], record["height"]),
                 "format: RGBA8888",
                 "filter: Nearest,Nearest",
                 "repeat: none"]
        for region in sorted(by_page[page_id], key=lambda r: r["path"]):
            name = region["path"].split("\\")[-1]
            lines += [name,
                      "  rotate: false",
                      "  xy: {}, {}".format(region["ax"], region["ay"]),
                      "  size: {}, {}".format(region["aw"], region["ah"]),
                      "  orig: {}, {}".format(region["aw"], region["ah"]),
                      "  offset: 0, 0",
                      "  index: -1"]
        chunks.append("\n".join(lines))
    # libGDX's TextureAtlasData only begins a new page after a blank line, so page
    # blocks must be separated by one or page two is read as a region name.
    return "\n\n".join(chunks) + "\n"


ANIMATIONS_JSON = os.path.join(ROOT, "resources", "raw", "pvz2", "animations.json")

# Which PAM path segment a section's animations must sit under. Without this a folder
# token alone matches the wrong rig: "peashooter" also exists under FULL/NPC as the
# cutscene character, which is not the gameplay plant.
ANIM_CATEGORY = {"plants": "plant", "zombies": "zombie",
                 "lawn": "mowers", "environment": "backgrounds"}


def load_animations():
    """(category, normalised folder) -> [animation record, ...]"""
    with open(ANIMATIONS_JSON, encoding="utf-8") as handle:
        data = json.load(handle)
    index = collections.defaultdict(list)
    for anim in data["animations"]:
        parts = anim["path"].split("/")
        if len(parts) > 3:
            index[(norm(parts[2]), norm(parts[3]))].append(anim)
    return index


def emit_animations(section, mapping, anim_index, out_dir, dry_run, stats):
    """Writes one json of clip metadata per entity. No frames are baked.

    Baking PAM to frames needs libPVZ's BakedAnimation, which needs a GL Texture, so it
    is renderer work rather than an extraction step. What is portable now is the clip
    table: name, canvas size and clip durations, pointing at the atlas this tool wrote.
    """
    category = ANIM_CATEGORY[section]
    written, missing = 0, []
    for entity in sorted(mapping):
        record = mapping[entity]
        folder = record["source_folder"].split("/")[-1]
        anims = anim_index.get((category, norm(folder)), [])
        if not anims:
            missing.append({"entity": entity, "roster": section + " animations",
                            "reason": "no PAM under {}/ for folder {}".format(
                                category, folder)})
            continue
        payload = {
            "entity": entity,
            "atlas": "textures/{}/{}".format(
                "lawn" if section == "lawn"
                else "environment" if section == "environment" else section,
                record["atlas"]),
            "source_folder": record["source_folder"],
            "animations": [
                {"name": a["name"], "pam": a["path"], "canvas": a.get("canvas"),
                 "clips": a.get("clips", {})}
                for a in sorted(anims, key=lambda a: a["path"])
            ],
        }
        record["animation_data"] = "animations/{}/{}.json".format(
            ANIM_DIR[section], norm(entity) or norm(folder))
        if not dry_run:
            os.makedirs(out_dir, exist_ok=True)
            target = os.path.join(out_dir, (norm(entity) or norm(folder)) + ".json")
            with open(target, "w", encoding="utf-8", newline="\n") as handle:
                json.dump(payload, handle, indent=2, sort_keys=True)
                handle.write("\n")
        written += 1
        stats["anim"] += 1
    return written, missing


# Only three animation folders are documented, so lawn and environment clips ride along
# with effects rather than inventing new directories.
ANIM_DIR = {"plants": "plants", "zombies": "zombies",
            "lawn": "effects", "environment": "effects"}


def page_filename(page_id):
    return page_id.replace("ATLASIMAGE_ATLAS_", "") + ".PNG"


def emit(entity_key, folder_entry, out_dir, pages, dry_run, stats):
    """Copies the entity's page(s) and writes its .atlas. Returns the record."""
    page_ids = sorted(folder_entry["pages"])
    page_files = {}
    for page_id in page_ids:
        src_name = page_filename(page_id)
        src = os.path.join(ATLAS_DIR, src_name)
        if not os.path.exists(src):
            return None
        dst_name = src_name.lower()
        page_files[page_id] = {"file": dst_name,
                               "width": pages[page_id].get("width", 1024),
                               "height": pages[page_id].get("height", 1024)}
        dst = os.path.join(out_dir, dst_name)
        if not dry_run:
            os.makedirs(out_dir, exist_ok=True)
            if not os.path.exists(dst):
                shutil.copyfile(src, dst)
                stats["png"] += 1
                stats["bytes"] += os.path.getsize(dst)
        else:
            stats["png"] += 1
            stats["bytes"] += os.path.getsize(src)

    names = [r["path"].split("\\")[-1] for r in folder_entry["regions"]]
    duplicates = sorted({n for n in names if names.count(n) > 1})

    atlas_name = entity_key + ".atlas"
    if not dry_run:
        os.makedirs(out_dir, exist_ok=True)
        with open(os.path.join(out_dir, atlas_name), "w", encoding="utf-8",
                  newline="\n") as handle:
            handle.write(atlas_text(page_ids, folder_entry["regions"], page_files))
    stats["atlas"] += 1

    return {"atlas": atlas_name,
            "pages": [page_files[p]["file"] for p in page_ids],
            "regions": len(folder_entry["regions"]),
            "duplicate_region_names": duplicates}


def run(dry_run):
    sprites, pages = load_resources()
    tree = index_folders(sprites)

    plant_folders = tree.get("plant", {})
    zombie_folders = tree.get("zombie", {})
    mower_folders = tree.get("mowers", {})
    background_folders = tree.get("backgrounds", {})

    plants_map, unresolved = match_plants(plant_folders)
    zombies_map, z_unresolved = match_zombies(zombie_folders)
    mowers_map, m_unresolved = match_worlds(mower_folders, "mower")
    worlds_map, w_unresolved = match_worlds(background_folders, "background")
    unresolved += z_unresolved + m_unresolved + w_unresolved

    stats = collections.Counter()
    mapping = {
        "generated_by": "tools/asset-extract/extract_assets.py",
        "source": "resources/raw/pvz2/RESOURCES.json",
        "resolution": RESOLUTION,
        "note": ("Region names are the upstream path leaf, unchanged. Atlas pages keep "
                 "their upstream file name, lowercased. Every runtime file here is a "
                 "byte copy of a file under resources/raw/pvz2/ATLASES."),
        "plants": {}, "zombies": {}, "lawn": {}, "environment": {},
        "effects": {},
        "unresolved": sorted(unresolved, key=lambda u: (u["roster"], u["entity"])),
    }

    targets = [
        ("plants", plants_map, plant_folders, os.path.join(ASSETS, "textures", "plants")),
        ("zombies", zombies_map, zombie_folders, os.path.join(ASSETS, "textures", "zombies")),
        ("lawn", mowers_map, mower_folders, os.path.join(ASSETS, "textures", "lawn")),
        ("environment", worlds_map, background_folders,
         os.path.join(ASSETS, "textures", "environment")),
    ]

    for section, resolved, folders, out_dir in targets:
        for entity in sorted(resolved):
            folder = resolved[entity]
            record = emit(norm(entity) or norm(folder), folders[folder], out_dir,
                          pages, dry_run, stats)
            if record is None:
                mapping["unresolved"].append(
                    {"entity": entity, "roster": section,
                     "reason": "atlas page for folder " + folder + " is not on disk"})
                continue
            record["source_folder"] = "images/{}/.../{}".format(RESOLUTION, folder)
            record["source_pages"] = sorted(folders[folder]["pages"])
            mapping[section][entity] = record

    anim_index = load_animations()
    for section, _, _, _ in targets:
        out_dir = os.path.join(ASSETS, "animations", ANIM_DIR[section])
        _, missing = emit_animations(section, mapping[section], anim_index, out_dir,
                                     dry_run, stats)
        mapping["unresolved"] += missing
    mapping["unresolved"] = sorted(mapping["unresolved"],
                                   key=lambda u: (u["roster"], u["entity"]))

    if not dry_run:
        os.makedirs(os.path.join(ASSETS, "metadata"), exist_ok=True)
        with open(os.path.join(ASSETS, "metadata", "asset-map.json"), "w",
                  encoding="utf-8", newline="\n") as handle:
            json.dump(mapping, handle, indent=2, sort_keys=True)
            handle.write("\n")

    print("plants      : {:3d} atlases".format(len(mapping["plants"])))
    print("zombies     : {:3d} atlases".format(len(mapping["zombies"])))
    print("lawn        : {:3d} atlases".format(len(mapping["lawn"])))
    print("environment : {:3d} atlases".format(len(mapping["environment"])))
    print("effects     : {:3d} atlases".format(len(mapping["effects"])))
    print("animations  : {:3d} clip files".format(stats["anim"]))
    print("png pages   : {:3d}".format(stats["png"]))
    print("bytes       : {:.1f} MB".format(stats["bytes"] / 1024 / 1024))
    print("unresolved  : {:3d}".format(len(mapping["unresolved"])))
    return 0


SEED_PACKET_PAGE = "ATLASIMAGE_ATLAS_UI_SEEDPACKETS_768_00"

# Plant name -> region leaf on the seed-packet page. Every pair was cropped out of the page and
# looked at against the plant it names. Unlike the per-entity PAM sheets, this atlas holds one
# finished portrait per plant, which is what the almanac and the seed chooser draw.
# Left out on purpose: Rotobaga, Cat-tail, Pierce-mint and catTail-mint have no packet at all,
# and "headbutter" is Headbutter Lettuce rather than the project's Iceberg Lettuce.
SEED_PACKETS = {
    "Sunflower": "sunflower", "Twin Sunflower": "twinsunflower", "Sun-shroom": "sunshroom",
    "Primal Sunflower": "primalsunflower", "Gold Bloom": "goldbloom", "Peashooter": "peashooter",
    "Repeater": "repeater", "Threepeater": "threepeater", "Snow Pea": "snowpea",
    "Pea Pod": "peapod", "Split Pea": "splitpea", "Citron": "citron",
    "Caulipower": "caulipower", "Electric Blueberry": "electricblueberry",
    "Bowling Bulb": "bowlingbulb", "Cactus": "cactus", "Fire Peashooter": "firepeashooter",
    "Starfruit": "starfruit", "Sea-shroom": "seashroom", "Puff-shroom": "puffshroom",
    "Fume-shroom": "fumeshroom", "Cabbage-pult": "cabbagepult", "Kernel-pult": "kernelpult",
    "Melon-pult": "Melonpult", "Winter Melon": "wintermelon", "Pepper-pult": "pepperpult",
    "Potato Mine": "potatomine", "Primal Potato Mine": "primalpotatomine",
    "Cherry Bomb": "cherry_bomb", "Squash": "squash", "Grapeshot": "grapeshot",
    "Jalapeno": "jalapeno", "Doom-shroom": "doomshroom", "Tangle Kelp": "tanglekelp",
    "Bonk Choy": "bonkchoy", "Phat Beet": "phatbeet", "Chomper": "chomper",
    "Wasabi Whip": "wasabiwhip", "Kiwibeast": "kiwibeast", "Wall-nut": "wallnut",
    "Tall-nut": "tallnut", "Endurian": "endurian", "Garlic": "garlic",
    "Sweet Potato": "sweetpotato", "Explode-o-nut": "explodeonut", "Pumpkin": "pumpkin",
    "Sun Bean": "sunbean", "Torchwood": "torchwood", "Magnet-shroom": "magnetshroom",
    "Hypno-shroom": "hypnoshroom", "Imitater": "imitater", "Ice-shroom": "iceshroom",
    "Lily Pad": "lilypad", "Hot Potato": "hotpotato", "Grave Buster": "gravebuster",
    "Enlighten-mint": "enlightenmint", "Appease-mint": "appeasemint", "Arma-mint": "armamint",
    "Bombard-mint": "bombardmint", "Enforce-mint": "enforcemint",
    "Reinforce-mint": "reinforcemint", "Enchant-mint": "enchantmint",
    "Mega Gatling Pea": "megagatling", "Goo Peashooter": "poisonpeashooter",
}


def extract_seed_packets(dry_run):
    """Copies the seed-packet page and writes an atlas holding only the verified portraits.

    Regions here are named by the normalised project plant name rather than the upstream leaf,
    which is the one place this tool departs from "keep the upstream name". Upstream spells them
    inconsistently ("Melonpult", "cherry_bomb", "poisonpeashooter"), so naming by the game's own
    key lets the UI look a plant up directly and keeps the verified pairing in this file only.
    """
    sprites, pages = load_resources()
    by_leaf = {s["path"].split("\\")[-1]: s
               for s in sprites if s["parent"] == SEED_PACKET_PAGE}

    picked, missing = [], []
    for plant, leaf in sorted(SEED_PACKETS.items()):
        region = by_leaf.get(leaf)
        if region is None:
            missing.append(plant + " (" + leaf + ")")
            continue
        picked.append((norm(plant), region))
    if missing:
        print("no region on the page for:", ", ".join(missing))

    src_name = page_filename(SEED_PACKET_PAGE)
    dst_name = src_name.lower()
    src = os.path.join(ATLAS_DIR, src_name)
    if not os.path.exists(src):
        print("missing " + src, file=sys.stderr)
        return 1

    out_dir = os.path.join(ASSETS, "textures", "plants")
    page = pages[SEED_PACKET_PAGE]
    lines = [dst_name,
             "size: {},{}".format(page.get("width", 1024), page.get("height", 1024)),
             "format: RGBA8888", "filter: Nearest,Nearest", "repeat: none"]
    for name, region in picked:
        lines += [name,
                  "  rotate: false",
                  "  xy: {}, {}".format(region["ax"], region["ay"]),
                  "  size: {}, {}".format(region["aw"], region["ah"]),
                  "  orig: {}, {}".format(region["aw"], region["ah"]),
                  "  offset: 0, 0",
                  "  index: -1"]

    if not dry_run:
        os.makedirs(out_dir, exist_ok=True)
        dst = os.path.join(out_dir, dst_name)
        if os.path.exists(dst):
            print("page already present, left alone:", dst_name)
        else:
            shutil.copyfile(src, dst)
        with open(os.path.join(out_dir, "seedpackets.atlas"), "w", encoding="utf-8",
                  newline="\n") as handle:
            handle.write("\n".join(lines) + "\n")

    print("seed packets: {} verified plants, {} regions written".format(
        len(SEED_PACKETS), len(picked)))
    return 0


ZOMBIE_PACKET_PAGE = "ATLASIMAGE_ATLAS_UI_ZOMBIEPACKETS_768_00"

# I, Zombie engine name -> region leaf on the zombie-packet page. Each pair was cropped out and
# looked at: they are all whole zombies, not hats or armour pieces. tutorial_armor4 is the zombie
# holding an orange door-like shield, which is the closest thing the archive has to a screen door.
# pole-vaulter, digger and ladder have no PvZ2 packet, so the game draws its "no art" state.
ZOMBIE_PACKETS = {
    "basic": "tutorial",
    "conehead": "tutorial_armor1",
    "buckethead": "tutorial_armor2",
    "screen-door": "tutorial_armor4",
    "newspaper": "modern_newspaper",
    "football": "modern_allstar",
    "gargantuar": "tutorial_gargantuar",
    "sun-imp": "tutorial_imp",
    # Ancient Egypt roster, keyed by the alias Zombies.json uses. Same check: every one of these
    # was cropped and looked at, and they are whole zombies.
    "ZombieMummyDefault": "mummy",
    "ZombieMummyArmor1Default": "mummy_armor1",
    "ZombieMummyArmor2Default": "mummy_armor2",
    "ZombieMummyArmor4Default": "mummy_armor4",
    "ZombieEgyptImpDefault": "egypt_imp",
    "ZombieRaDefault": "ra",
    "ZombieTombRaiserDefault": "tomb_raiser",
    "ZombieEgyptGargantuar": "egypt_gargantuar",
    # Frostbite Caves roster, checked the same way: whole zombies, not props.
    "ZombieIceAgeDodo": "iceage_dodo",
    "ZombieIceAgeHunter": "iceage_hunter",
    "ZombieIceAgeTroglobite": "iceage_troglobite",
    "ZombieZombossMechCowboy": "zombossmech_cowboy",
    # Big Wave Beach roster, checked the same way: whole zombies, not props.
    "ZombieBeachFisherman": "beach_fisherman",
    "ZombieBeachOctopus": "beach_octopus",
    "ZombieBeachSnorkel": "beach_snorkel",
    "ZombieZombossMechPirate": "zombossmech_pirate",
}


def extract_zombie_packets(dry_run):
    """Writes an atlas of the verified zombie portraits, named by the I, Zombie engine key."""
    sprites, pages = load_resources()
    by_leaf = {s["path"].split("\\")[-1]: s
               for s in sprites if s["parent"] == ZOMBIE_PACKET_PAGE}

    picked, missing = [], []
    for name, leaf in sorted(ZOMBIE_PACKETS.items()):
        region = by_leaf.get(leaf)
        if region is None:
            missing.append(name + " (" + leaf + ")")
            continue
        picked.append((name, region))
    if missing:
        print("no region on the page for:", ", ".join(missing))

    src_name = page_filename(ZOMBIE_PACKET_PAGE)
    dst_name = src_name.lower()
    src = os.path.join(ATLAS_DIR, src_name)
    if not os.path.exists(src):
        print("missing " + src, file=sys.stderr)
        return 1

    out_dir = os.path.join(ASSETS, "textures", "zombies")
    page = pages[ZOMBIE_PACKET_PAGE]
    lines = [dst_name,
             "size: {},{}".format(page.get("width", 1024), page.get("height", 1024)),
             "format: RGBA8888", "filter: Nearest,Nearest", "repeat: none"]
    for name, region in picked:
        lines += [name,
                  "  rotate: false",
                  "  xy: {}, {}".format(region["ax"], region["ay"]),
                  "  size: {}, {}".format(region["aw"], region["ah"]),
                  "  orig: {}, {}".format(region["aw"], region["ah"]),
                  "  offset: 0, 0",
                  "  index: -1"]

    if not dry_run:
        os.makedirs(out_dir, exist_ok=True)
        dst = os.path.join(out_dir, dst_name)
        if os.path.exists(dst):
            print("page already present, left alone:", dst_name)
        else:
            shutil.copyfile(src, dst)
        with open(os.path.join(out_dir, "zombiepackets.atlas"), "w", encoding="utf-8",
                  newline="\n") as handle:
            handle.write("\n".join(lines) + "\n")

    print("zombie packets: {} verified zombies, {} regions written".format(
        len(ZOMBIE_PACKETS), len(picked)))
    return 0


# Small in-game pieces the renderer needs, each one cropped and looked at before being listed here.
# They live on four different upstream pages, so this mode composes them into one small sheet
# instead of copying four 1024px pages into assets/.
HUD_PIECES = {
    "sun": ("ATLASIMAGE_ATLAS_UI_ALWAYSLOADED_768_01", "hud_ingame", "sun"),
    "plantfood": ("ATLASIMAGE_ATLAS_UI_ALMANAC_STATICONS_768_00", "almanac_staticons",
                  "almanac_stat_icon_plantfood_large"),
    "lawnmower": ("ATLASIMAGE_ATLAS_LEVELCOMMON_768_00", "initial", "card_mower"),
    "pea": ("ATLASIMAGE_ATLAS_PLANTPEASHOOTER_768_00", "peashooter", "peashooter_33x35"),
    "gravestone": ("ATLASIMAGE_ATLAS_EGYPT_GRAVESTONE_768_00", "egypt_gravestone",
                   "Egypt_Hieroglyph_118x148"),
}


def extract_hud(dry_run):
    """Composes the verified HUD/board pieces into assets/textures/ui/hud.png + hud.atlas."""
    from PIL import Image

    sprites, _ = load_resources()
    by_leaf = {}
    for sprite in sprites:
        by_leaf.setdefault(sprite["path"].split("\\")[-1], []).append(sprite)

    picked, missing = [], []
    for name, (page_id, _folder, leaf) in sorted(HUD_PIECES.items()):
        match = None
        for candidate in by_leaf.get(leaf, []):
            if candidate["parent"] == page_id:
                match = candidate
                break
        if match is None:
            missing.append(name + " (" + leaf + ")")
        else:
            picked.append((name, match))
    if missing:
        print("no region found for:", ", ".join(missing))

    pad = 2
    width = sum(s["aw"] + pad for _, s in picked) + pad
    height = max(s["ah"] for _, s in picked) + pad * 2
    sheet = Image.new("RGBA", (width, height), (0, 0, 0, 0))

    lines = ["hud.png", "size: {},{}".format(width, height),
             "format: RGBA8888", "filter: Linear,Linear", "repeat: none"]
    cache = {}
    cursor = pad
    for name, sprite in picked:
        page_file = page_filename(sprite["parent"])
        source = os.path.join(ATLAS_DIR, page_file)
        if source not in cache:
            cache[source] = Image.open(source).convert("RGBA")
        box = (sprite["ax"], sprite["ay"], sprite["ax"] + sprite["aw"], sprite["ay"] + sprite["ah"])
        sheet.paste(cache[source].crop(box), (cursor, pad))
        lines += [name,
                  "  rotate: false",
                  "  xy: {}, {}".format(cursor, pad),
                  "  size: {}, {}".format(sprite["aw"], sprite["ah"]),
                  "  orig: {}, {}".format(sprite["aw"], sprite["ah"]),
                  "  offset: 0, 0",
                  "  index: -1"]
        cursor += sprite["aw"] + pad

    if not dry_run:
        out_dir = os.path.join(ASSETS, "textures", "ui")
        os.makedirs(out_dir, exist_ok=True)
        sheet.save(os.path.join(out_dir, "hud.png"))
        with open(os.path.join(out_dir, "hud.atlas"), "w", encoding="utf-8",
                  newline=NEWLINE) as handle:
            handle.write(NEWLINE.join(lines) + NEWLINE)

    print("hud sheet: {} pieces, {}x{}".format(len(picked), width, height))
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true",
                        help="report what would be written without touching assets/")
    parser.add_argument("--seed-packets", action="store_true",
                        help="only extract the seed-packet portraits used by the Collection")
    parser.add_argument("--zombie-packets", action="store_true",
                        help="only extract the zombie portraits used by I, Zombie")
    parser.add_argument("--hud", action="store_true",
                        help="only compose the in-game sun/mower/grave/pea sheet")
    args = parser.parse_args()
    if args.seed_packets:
        return extract_seed_packets(args.dry_run)
    if args.zombie_packets:
        return extract_zombie_packets(args.dry_run)
    if args.hud:
        return extract_hud(args.dry_run)
    if not os.path.exists(RESOURCES_JSON):
        print("missing " + RESOURCES_JSON, file=sys.stderr)
        return 1
    return run(args.dry_run)


if __name__ == "__main__":
    raise SystemExit(main())
