#!/usr/bin/env python3
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

import mca
from mca import nbt


TARGET_NAMESPACES = {"upgrade_aquatic", "woodworks"}


def normalize_chest_name(name: str) -> str | None:
    if ":" not in name:
        return None
    namespace, path = name.split(":", 1)
    if namespace not in TARGET_NAMESPACES:
        return None
    if path.endswith("_trapped_chest"):
        return "minecraft:trapped_chest"
    if path.endswith("_chest"):
        return "minecraft:chest"
    return None


def discover_region_dirs(world_root: Path) -> list[Path]:
    result = []
    direct = [
        world_root / "region",
        world_root / "DIM-1" / "region",
        world_root / "DIM1" / "region",
    ]
    for path in direct:
        if path.is_dir():
            result.append(path)
    dimensions_root = world_root / "dimensions"
    if dimensions_root.is_dir():
        for path in dimensions_root.rglob("region"):
            if path.is_dir():
                result.append(path)
    seen = []
    for path in result:
        if path not in seen:
            seen.append(path)
    return seen


def convert_palette_entry(entry: nbt.TAG_Compound) -> bool:
    try:
        name = entry["Name"].value
    except KeyError:
        return False
    mapped = normalize_chest_name(name)
    if not mapped:
        return False
    entry["Name"] = nbt.TAG_String(mapped, name="Name")
    try:
        properties = entry["Properties"]
    except KeyError:
        return True
    if not isinstance(properties, nbt.TAG_Compound):
        entry.pop("Properties", None)
        return True
    allowed = nbt.TAG_Compound(name="Properties")
    for key in ("facing", "type", "waterlogged"):
        if key in properties:
            allowed[key] = properties[key]
    if allowed:
        entry["Properties"] = allowed
    else:
        entry.pop("Properties", None)
    return True


def convert_block_entity(entity: nbt.TAG_Compound) -> bool:
    try:
        entity_id = entity["id"].value
    except KeyError:
        return False
    mapped = normalize_chest_name(entity_id)
    if not mapped:
        return False
    entity["id"] = nbt.TAG_String(mapped, name="id")
    entity.pop("OpenNess", None)
    entity.pop("openNess", None)
    return True


def process_chunk(chunk: mca.Chunk) -> tuple[int, int]:
    changed_palette = 0
    changed_block_entities = 0
    chunk_data = chunk.data

    if "sections" in chunk_data:
        for section in chunk_data["sections"]:
            if "block_states" not in section:
                continue
            block_states = section["block_states"]
            if "palette" not in block_states:
                continue
            for entry in block_states["palette"]:
                if isinstance(entry, nbt.TAG_Compound) and convert_palette_entry(entry):
                    changed_palette += 1

    if "block_entities" in chunk_data:
        for entity in chunk_data["block_entities"]:
            if isinstance(entity, nbt.TAG_Compound) and convert_block_entity(entity):
                changed_block_entities += 1

    return changed_palette, changed_block_entities


def process_region_file(path: Path, backup_root: Path | None, dry_run: bool) -> tuple[int, int, int]:
    region = mca.Region.from_file(str(path))
    changed_chunks = 0
    changed_palette = 0
    changed_block_entities = 0
    output = mca.EmptyRegion(*parse_region_coords(path))

    for local_z in range(32):
        for local_x in range(32):
            chunk_x = output.x * 32 + local_x
            chunk_z = output.z * 32 + local_z
            location = region.chunk_location(chunk_x, chunk_z)
            if location == (0, 0):
                continue
            chunk = region.get_chunk(chunk_x, chunk_z)
            palette_changes, block_entity_changes = process_chunk(chunk)
            if palette_changes > 0 or block_entity_changes > 0:
                changed_chunks += 1
                changed_palette += palette_changes
                changed_block_entities += block_entity_changes
            if not dry_run:
                output.add_chunk(chunk)

    if changed_chunks > 0 and not dry_run:
        if backup_root is not None:
            backup_root.mkdir(parents=True, exist_ok=True)
            backup_path = backup_root / path.name
            if not backup_path.exists():
                shutil.copy2(path, backup_path)
        output.save(str(path))

    return changed_chunks, changed_palette, changed_block_entities


def parse_region_coords(path: Path) -> tuple[int, int]:
    parts = path.stem.split(".")
    if len(parts) != 3 or parts[0] != "r":
        raise ValueError(f"Unexpected region filename: {path.name}")
    return int(parts[1]), int(parts[2])


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert Upgrade Aquatic and Woodworks chest blocks to vanilla chests while preserving contents."
    )
    parser.add_argument("world", help="Path to the world folder")
    parser.add_argument("--dry-run", action="store_true", help="Scan and report changes without writing")
    parser.add_argument("--no-backup", action="store_true", help="Do not create backups before writing")
    args = parser.parse_args()

    world_root = Path(args.world)
    if not world_root.is_dir():
        print(f"World folder not found: {world_root}", file=sys.stderr)
        return 1

    region_dirs = discover_region_dirs(world_root)
    if not region_dirs:
        print("No region directories found.", file=sys.stderr)
        return 1

    backup_root = None if args.no_backup or args.dry_run else world_root / "_chest_conversion_backup"
    total_regions = 0
    total_chunks = 0
    total_palette = 0
    total_block_entities = 0

    for region_dir in region_dirs:
        print(f"Scanning {region_dir}")
        for region_file in sorted(region_dir.glob("r.*.*.mca")):
            try:
                changed_chunks, changed_palette, changed_block_entities = process_region_file(
                    region_file,
                    None if backup_root is None else backup_root / region_dir.relative_to(world_root),
                    args.dry_run,
                )
            except Exception as exc:
                print(f"ERROR {region_file}: {exc}", file=sys.stderr)
                continue
            if changed_chunks > 0:
                total_regions += 1
                total_chunks += changed_chunks
                total_palette += changed_palette
                total_block_entities += changed_block_entities
                print(
                    f"  updated {region_file.name}: {changed_chunks} chunk(s), "
                    f"{changed_palette} palette entry change(s), "
                    f"{changed_block_entities} block entity change(s)"
                )

    if args.dry_run:
        print(
            f"Dry run complete. Would update {total_regions} region file(s), "
            f"{total_chunks} chunk(s), {total_palette} palette entry change(s), "
            f"{total_block_entities} block entity change(s)."
        )
    else:
        print(
            f"Done. Updated {total_regions} region file(s), {total_chunks} chunk(s), "
            f"{total_palette} palette entry change(s), {total_block_entities} block entity change(s)."
        )
        if backup_root is not None:
            print(f"Backups written to: {backup_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
