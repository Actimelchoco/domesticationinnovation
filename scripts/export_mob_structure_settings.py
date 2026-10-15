"""Export readable config excerpts plus original files for a later manual merge."""
import hashlib
import json
from pathlib import Path
import re
import shutil
import tomllib
import zipfile

SOURCE = Path(r"C:\Users\bruns\Documents\linggango server")
DEST = Path(r"C:\Users\bruns\Downloads\Mods\linggango-old-spawn-structure-settings")
DEST.mkdir(parents=True, exist_ok=True)

STRUCTURE = re.compile(r"structur|world.?gen|spacing|separation", re.I)
SPAWN = re.compile(r"spawn|population|mob.?cap", re.I)
EXCLUDE = re.compile(r"respawn|summon|projectile|fire.ring|bonanza|teleport|spawn.position|spawn.point", re.I)

def literal(value):
    if isinstance(value, str):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, bool):
        return str(value).lower()
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, list):
        return "[" + ", ".join(literal(item) for item in value) + "]"
    if isinstance(value, dict):
        return "{ " + ", ".join(literal(key) + " = " + literal(item) for key, item in value.items()) + " }"
    raise ValueError(f"Unsupported TOML value: {value!r}")

def entries(data, prefix=()):
    for key, value in data.items():
        if isinstance(value, dict):
            yield from entries(value, prefix + (key,))
        else:
            yield prefix, key, value

def toml_excerpt(selected):
    result, previous = [], None
    for section, key, value in selected:
        if section != previous:
            if section:
                result.append("\n[" + ".".join(literal(part) for part in section) + "]")
            previous = section
        result.append(literal(key) + " = " + literal(value))
    return "\n".join(result) + "\n"

output = {"mob": [], "structure": []}
manifest, errors = [], []
configs = [SOURCE / "config", SOURCE / "defaultconfigs", SOURCE / "newWorld/serverconfig"]
for directory in configs:
    for file in sorted(directory.rglob("*")):
        if not file.is_file() or file.suffix.lower() not in (".toml", ".json", ".json5", ".jsonc"):
            continue
        relative = file.relative_to(SOURCE)
        lower = str(relative).lower()
        if ("client" in file.name.lower() or "ftbquests" in lower
                or any(term in lower for term in ("subtle_effects", "crash_assistant", "corpse-", "sophisticatedbackpacks",
                        "evolvedmekanism", "experienceobelisk", "composite_material", "lootr-", "compactmachines",
                        "easy_villagers", "explorerscompass", "naturescompass", "universalenchants"))):
            continue
        text = file.read_text(encoding="utf-8-sig")
        categories = {}
        if file.suffix.lower() == ".toml":
            try:
                data = tomllib.loads(text)
            except tomllib.TOMLDecodeError as error:
                errors.append({"file": str(relative), "error": str(error)})
                continue
            selections = {"mob": [], "structure": []}
            for section, key, value in entries(data):
                scope = " ".join(section + (key,))
                # Structures/Worldgen sections include their placement/loot options.
                if STRUCTURE.search(scope) and not re.search(r"ore|sapling|tree|flower|spell|glyph|ritual|recipe", scope, re.I):
                    selections["structure"].append((section, key, value))
                if SPAWN.search(scope) and not EXCLUDE.search(scope) and not re.search(r"spawner|egg|charge|damage|particle|sound|animation|cooldown.*attack", scope, re.I):
                    selections["mob"].append((section, key, value))
                elif file.name == "mob_spawn_fix-common.toml":
                    selections["mob"].append((section, key, value))
            for category, selection in selections.items():
                if not selection:
                    continue
                excerpt = toml_excerpt(selection)
                # Ensure excerpt round-trips to exactly the selected source values.
                reparsed = tomllib.loads(excerpt)
                assert list(entries(reparsed)) == selection, relative
                categories[category] = excerpt
        else:
            # Dedicated spawn/biome tables and structure configs retain their syntax.
            if (re.search(r"spawn|mob_spawn|crittersandcompanions", lower)
                    or "/spawning" in lower.replace("\\", "/")):
                categories["mob"] = text
            if re.search(r"structure|structurify|medievalend|medieval_nether|idas|dungeon", file.name, re.I):
                categories["structure"] = text
        if not categories:
            continue
        original = DEST / "original-configs" / relative
        original.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, original)
        manifest.append({"source": str(relative), "categories": list(categories),
                         "sha256": hashlib.sha256(file.read_bytes()).hexdigest()})
        for category, excerpt in categories.items():
            output[category].append(f"\n{'=' * 78}\nSOURCE: {relative}\n{'=' * 78}\n{excerpt}")
            snippet = DEST / "merge-excerpts" / category / relative
            snippet.parent.mkdir(parents=True, exist_ok=True)
            snippet.write_text(excerpt, encoding="utf-8")

for category, filename in (("mob", "MOB_SPAWNING_SETTINGS.txt"), ("structure", "STRUCTURE_SETTINGS.txt")):
    introduction = (f"SOURCE: {SOURCE}\n"
                    "Exact source values, grouped by original file path. TOML excerpts must be merged\n"
                    "into the new pack's existing configs; do not replace full configs with excerpts.\n"
                    "Dedicated JSON/JSON5/JSONC files are reproduced in full and may also contain\n"
                    "related behavior options. Requires compatible mod versions.\n"
                    "Configs can be stale; see README.txt for missing controller mods.\n")
    (DEST / filename).write_text(introduction + "".join(output[category]), encoding="utf-8")

mod_jars = sorted(p.name for p in (SOURCE / "mods").glob("*.jar"))
(DEST / "SOURCE_MOD_VERSIONS.txt").write_text("\n".join(mod_jars) + "\n", encoding="utf-8")
(DEST / "manifest.json").write_text(json.dumps({"source": str(SOURCE), "files": manifest, "parse_errors": errors}, indent=2), encoding="utf-8")
(DEST / "README.txt").write_text('''Export source: C:\\Users\\bruns\\Documents\\linggango server

MOB_SPAWNING_SETTINGS.txt and STRUCTURE_SETTINGS.txt contain grouped settings.
merge-excerpts/ contains per-file excerpts. Merge matching keys/sections into
the new pack's configs. These TOML files are partial, not drop-in replacements.
original-configs/ contains exact originals for reference; replacing them wholesale
also replaces unrelated settings. manifest.json records source paths and hashes.

GLOBAL MOB SPAWNING: source uses mob_spawn_fix-0.1.jar with
config/mob_spawn_fix-common.toml. Its multipliers, crowding caps, fallback biome
injection, exclusions and dynamic balancing are all included. The config requires
that mod; copying it alone does nothing if the new pack does not have the mod.
Per-mod configs also control natural spawning before global adjustments.
Do not add a second global spawn controller without checking their interaction.

STRUCTURES: most placement definitions are inside individual mod JARs; configs
alone do not reproduce changes to those definitions between mod versions.
The source has Structure Essentials and Structure Layout Optimizer installed.
Companions has its own biome settings. Sparse Structures and Structurify configs
are present, but their named JARs are absent from this source's mods folder.
Those configs are exported as references, not as confirmed active settings.

COMPANIONS: merge Spawn Rates and Structures from the companions.toml excerpts.
Both files may include related biome settings, since those affect both systems.

WORLD CONFIGS: newWorld/serverconfig/ contains existing-world overrides. Apply
matching settings in the target world's serverconfig. defaultconfigs/ supplies
defaults for new worlds, not automatic updates to existing worlds.

SOURCE_MOD_VERSIONS.txt lists exact source JAR filenames for compatibility checks.
No JARs are included, and no configs in either modpack were modified.
No matching KubeJS natural-spawn/worldgen event scripts were found; source
moonlight-global-datapacks/ and newWorld/datapacks/ were empty.

Restart after merging. Structure placement changes affect new/regenerated chunks.
''', encoding="utf-8")
archive = DEST.with_suffix(".zip")
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as target:
    for file in sorted(DEST.rglob("*")):
        if file.is_file():
            target.write(file, Path(DEST.name) / file.relative_to(DEST))
print(json.dumps({"folder": str(DEST), "zip": str(archive), "source_files": len(manifest),
                  "mob_files": len(output["mob"]), "structure_files": len(output["structure"]), "parse_errors": errors}, indent=2))
