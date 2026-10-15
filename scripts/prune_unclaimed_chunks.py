#!/usr/bin/env python3
"""Offline FTB Chunks 2001.3.x pruner. Standard library only, Python 3.10+.

Place in the world directory. Preview: python prune_unclaimed_chunks.py 25
Apply: python prune_unclaimed_chunks.py 25 --apply
Restore: python prune_unclaimed_chunks.py --restore chunk-pruning-backups/<id>
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from contextlib import contextmanager
from dataclasses import dataclass
import json
import mmap
import os
from pathlib import Path
import re
import shutil
import struct
import sys
import tempfile
import time
import uuid


class SNBT:
    """Strict reader for FTB's SNBT, including its optional comma syntax."""
    def __init__(self, text):
        self.text, self.pos = text, 0

    def space(self):
        while self.pos < len(self.text):
            if self.text[self.pos].isspace():
                self.pos += 1
            elif self.text.startswith("//", self.pos) or self.text[self.pos] == "#":
                end = self.text.find("\n", self.pos)
                self.pos = len(self.text) if end < 0 else end + 1
            else:
                break

    def token(self, key=False):
        self.space()
        if self.pos >= len(self.text):
            raise ValueError("Unexpected end of SNBT")
        quote = self.text[self.pos]
        if quote in "\"'":
            self.pos += 1
            result = []
            while self.pos < len(self.text):
                char = self.text[self.pos]
                self.pos += 1
                if char == quote:
                    return "".join(result)
                if char == "\\":
                    if self.pos >= len(self.text):
                        raise ValueError("Unterminated escape")
                    char = self.text[self.pos]
                    self.pos += 1
                    if char not in (quote, "\\"):
                        raise ValueError("Unsupported SNBT escape")
                result.append(char)
            raise ValueError("Unterminated string")
        start = self.pos
        delimiters = "{}[],;" + (":" if key else "")
        while self.pos < len(self.text) and not self.text[self.pos].isspace() and self.text[self.pos] not in delimiters:
            self.pos += 1
        if start == self.pos:
            raise ValueError(f"Unexpected token at offset {self.pos}")
        return self.text[start:self.pos]

    def value(self):
        self.space()
        if self.pos >= len(self.text):
            raise ValueError("Missing value")
        char = self.text[self.pos]
        if char == "{":
            self.pos += 1
            result = {}
            while True:
                self.space()
                if self.pos < len(self.text) and self.text[self.pos] == "}":
                    self.pos += 1
                    return result
                key = self.token(key=True)
                self.space()
                if self.pos >= len(self.text) or self.text[self.pos] != ":":
                    raise ValueError("Missing colon")
                self.pos += 1
                if key in result:
                    raise ValueError(f"Duplicate key: {key}")
                result[key] = self.value()
                self.space()
                if self.pos < len(self.text) and self.text[self.pos] == ",":
                    self.pos += 1
        if char == "[":
            self.pos += 1
            self.space()
            if re.match(r"[BILbil];", self.text[self.pos:self.pos + 2]):
                self.pos += 2
            result = []
            while True:
                self.space()
                if self.pos < len(self.text) and self.text[self.pos] == "]":
                    self.pos += 1
                    return result
                result.append(self.value())
                self.space()
                if self.pos < len(self.text) and self.text[self.pos] == ",":
                    self.pos += 1
        quoted = char in "\"'"
        token = self.token()
        if not quoted and re.fullmatch(r"[+-]?\d+[bBsSlL]?", token):
            return int(token.rstrip("bBsSlL"))
        return token

    def read(self):
        result = self.value()
        self.space()
        if self.pos != len(self.text):
            raise ValueError("Trailing SNBT data")
        return result


def load_claims(root):
    folder = root / "ftbchunks"
    files = sorted(folder.glob("*.snbt"))
    if not files:
        raise ValueError("No world/ftbchunks/*.snbt files found; refusing to assume there are no claims.")
    claims = defaultdict(set)
    for file in files:
        try:
            data = SNBT(file.read_text(encoding="utf-8-sig")).read()
            if not isinstance(data, dict) or not isinstance(data.get("chunks"), dict):
                raise ValueError("Missing authoritative top-level chunks compound")
            for dimension, entries in data["chunks"].items():
                if not re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", dimension) or not isinstance(entries, list):
                    raise ValueError("Invalid dimension or claim list")
                for entry in entries:
                    if not isinstance(entry, dict) or type(entry.get("x")) is not int or type(entry.get("z")) is not int:
                        raise ValueError("Invalid claim coordinates")
                    x, z = entry["x"], entry["z"]
                    if not (-2147483648 <= x <= 2147483647 and -2147483648 <= z <= 2147483647):
                        raise ValueError("Claim coordinate out of range")
                    claims[dimension].add((x, z))
        except (ValueError, OSError) as error:
            raise ValueError(f"Cannot read claims from {file}: {error}") from error
    return dict(claims)


@contextmanager
def world_lock(root):
    """Use the same byte-range lock as Java DirectoryLock/session.lock."""
    file = checked(root, root / "session.lock").open("a+b")
    locked = False
    try:
        if file.seek(0, 2) == 0:
            file.write(b"\xe2\x98\x83")
            file.flush()
        file.seek(0)
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(file.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.lockf(file.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            locked = True
        except OSError as error:
            raise RuntimeError("World is locked. Stop the server completely before running this script.") from error
        yield
    finally:
        if locked:
            file.seek(0)
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(file.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                import fcntl
                fcntl.lockf(file.fileno(), fcntl.LOCK_UN)
        file.close()


REGION = re.compile(r"r\.(-?\d+)\.(-?\d+)\.mca")
EXTERNAL = re.compile(r"c\.(-?\d+)\.(-?\d+)\.mcc")


def dimensions(root):
    result = {"minecraft:overworld": root}
    for name, folder in (("minecraft:the_nether", "DIM-1"), ("minecraft:the_end", "DIM1")):
        if (root / folder).is_dir():
            result[name] = root / folder
    custom = root / "dimensions"
    if custom.exists():
        for storage in custom.rglob("*"):
            if storage.is_dir() and storage.name in ("region", "entities", "poi"):
                parts = storage.parent.relative_to(custom).parts
                if len(parts) < 2:
                    raise ValueError(f"Unrecognized dimension storage: {storage}")
                name = parts[0] + ":" + "/".join(parts[1:])
                if name in result and result[name] != storage.parent:
                    raise ValueError(f"Ambiguous dimension storage: {name}")
                result[name] = storage.parent
    return result


def checked(root, path):
    # Resolve symbolic links too, to keep all mutation and backups inside the world.
    if not path.resolve().is_relative_to(root):
        raise ValueError(f"Path escapes world: {path}")
    if path.is_symlink():
        raise ValueError(f"Symbolic file links are not supported: {path}")
    return path


def preserve(x, z, claims, distance):
    return any(abs(x - cx) <= distance and abs(z - cz) <= distance for cx, cz in claims)


@dataclass
class Action:
    file: Path
    locations: tuple | None
    timestamps: bytes | None
    remove: set
    size: int
    kept_size: int

    @property
    def whole(self):
        return self.kept_size == 0


def plan(root, claims, distance):
    actions, terrain, empty, scanned = [], 0, 0, 0
    last = time.monotonic()
    for dimension, directory in sorted(dimensions(root).items()):
        positions = claims.get(dimension, set())
        print(f"Scanning {dimension}: {len(positions)} claims", flush=True)
        for storage in ("region", "entities", "poi"):
            folder = checked(root, directory / storage)
            if not folder.exists():
                continue
            for file in sorted(folder.iterdir()):
                match = REGION.fullmatch(file.name)
                ext = EXTERNAL.fullmatch(file.name)
                if not match and not ext:
                    continue
                checked(root, file)
                size = file.stat().st_size
                if ext:
                    x, z = map(int, ext.groups())
                    if not preserve(x, z, positions, distance):
                        actions.append(Action(file, None, None, set(), size, 0))
                    continue
                scanned += 1
                if not size:
                    empty += 1
                    continue
                if size < 8192:
                    raise ValueError(f"Region header is truncated: {file}")
                rx, rz = map(int, match.groups())
                nearby = [(x, z) for x, z in positions
                          if rx * 32 - distance <= x <= rx * 32 + 31 + distance
                          and rz * 32 - distance <= z <= rz * 32 + 31 + distance]
                removed, used, kept_sectors = set(), {0, 1}, 0
                with file.open("rb") as handle, mmap.mmap(handle.fileno(), 0, access=mmap.ACCESS_READ) as data:
                    locations = struct.unpack_from(">1024I", data)
                    timestamps = data[4096:8192]
                    for index, location in enumerate(locations):
                        if not location:
                            continue
                        offset, sectors = location >> 8, location & 255
                        start = offset * 4096
                        if offset < 2 or not sectors or start + 5 > size or any(s in used for s in range(offset, offset + sectors)):
                            raise ValueError(f"Invalid region allocation: {file}, entry {index}")
                        used.update(range(offset, offset + sectors))
                        length = struct.unpack_from(">I", data, start)[0]
                        if length < 1 or length + 4 > sectors * 4096 or start + length + 4 > size:
                            raise ValueError(f"Truncated chunk payload: {file}, entry {index}")
                        x, z = rx * 32 + index % 32, rz * 32 + index // 32
                        if preserve(x, z, nearby, distance):
                            kept_sectors += sectors
                        else:
                            removed.add(index)
                if removed:
                    kept_size = (2 + kept_sectors) * 4096 if kept_sectors else 0
                    actions.append(Action(file, locations, timestamps, removed, size, kept_size))
                    if storage == "region":
                        terrain += len(removed)
                if time.monotonic() - last >= 2:
                    print(f"  {scanned:,} region files scanned; {terrain:,} terrain chunks selected", flush=True)
                    last = time.monotonic()
    print(f"Scanned {scanned:,} region files; skipped {empty:,} empty placeholders.")
    return actions, terrain


def compact(action, source):
    header = bytearray(8192)
    with source.open("rb") as handle, mmap.mmap(handle.fileno(), 0, access=mmap.ACCESS_READ) as data:
        descriptor, temp_name = tempfile.mkstemp(prefix="claim-prune-", suffix=".tmp", dir=action.file.parent)
        temp = Path(temp_name)
        try:
            with os.fdopen(descriptor, "wb") as output:
                output.write(header)
                sector = 2
                for index, location in enumerate(action.locations):
                    if not location or index in action.remove:
                        continue
                    offset, sectors = location >> 8, location & 255
                    start, length = offset * 4096, sectors * 4096
                    payload = data[start:min(start + length, len(data))]
                    output.write(payload)
                    output.write(b"\0" * (length - len(payload)))
                    struct.pack_into(">I", header, index * 4, sector << 8 | sectors)
                    header[4096 + index * 4:4100 + index * 4] = action.timestamps[index * 4:index * 4 + 4]
                    sector += sectors
                output.seek(0)
                output.write(header)
                output.flush()
                os.fsync(output.fileno())
            os.replace(temp, action.file)
        finally:
            temp.unlink(missing_ok=True)


def restore(root, backup):
    backup = checked(root, backup.resolve())
    manifest = json.loads((backup / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("format") != 1:
        raise ValueError("Unsupported backup manifest")
    pairs = []
    for entry in manifest["files"]:
        relative = Path(entry)
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError("Invalid backup path")
        source, target = checked(root, backup / relative), checked(root, root / relative)
        if source.is_file():
            pairs.append((source, target))
    for index, (source, target) in enumerate(pairs, 1):
        target.parent.mkdir(parents=True, exist_ok=True)
        descriptor, temp_name = tempfile.mkstemp(prefix="claim-restore-", suffix=".tmp", dir=target.parent)
        os.close(descriptor)
        try:
            shutil.copy2(source, temp_name)
            os.replace(temp_name, target)
        finally:
            Path(temp_name).unlink(missing_ok=True)
        if index % 100 == 0:
            print(f"Restored {index:,}/{len(pairs):,} files", flush=True)
    print(f"Restored {len(pairs):,} files. Backup retained: {backup}")


def apply(root, actions, terrain, distance):
    if not actions:
        print("Nothing to delete.")
        return None
    backup = root / "chunk-pruning-backups" / (time.strftime("%Y%m%d-%H%M%S-") + uuid.uuid4().hex[:8])
    checked(root, backup)
    backup.mkdir(parents=True)
    manifest = {"format": 1, "distance": distance, "terrain_chunks": terrain,
                "files": [str(action.file.relative_to(root)) for action in actions]}
    with (backup / "manifest.json").open("w", encoding="utf-8") as handle:
        json.dump(manifest, handle, indent=2)
        handle.flush()
        os.fsync(handle.fileno())
    print(f"Backup: {backup}", flush=True)
    try:
        # Whole files move into the backup instantly on the same disk. Only
        # regions with survivors require a full backup copy and compaction.
        last = time.monotonic()
        for index, action in enumerate(actions, 1):
            target = backup / action.file.relative_to(root)
            target.parent.mkdir(parents=True, exist_ok=True)
            if action.whole:
                os.replace(action.file, target)
            else:
                descriptor, temp_name = tempfile.mkstemp(prefix="backup-", suffix=".tmp", dir=target.parent)
                os.close(descriptor)
                try:
                    shutil.copy2(action.file, temp_name)
                    os.replace(temp_name, target)
                finally:
                    Path(temp_name).unlink(missing_ok=True)
                compact(action, target)
            if time.monotonic() - last >= 2 or index == len(actions):
                print(f"Processed {index:,}/{len(actions):,} affected files", flush=True)
                last = time.monotonic()
    except BaseException:
        print("Cleanup interrupted/failed; restoring backed-up files...", flush=True)
        restore(root, backup)
        raise
    (backup / "COMPLETE.txt").write_text(f"Removed {terrain} terrain chunks. Distance {distance}.\n", encoding="utf-8")
    print(f"COMPLETE: deleted {terrain:,} terrain chunks plus associated entity/POI data.")
    print("Backups still occupy disk space. Keep them until you have checked the world.")
    return backup


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("distance", nargs="?", type=int, help="Inclusive square buffer in chunks around every claim")
    parser.add_argument("--apply", action="store_true", help="Delete selected chunks; default is preview only")
    parser.add_argument("--world", type=Path, default=Path(__file__).resolve().parent, help="World folder; default is script's folder")
    parser.add_argument("--restore", type=Path, help="Restore a backup made by this script, relative to world folder")
    args = parser.parse_args(argv)
    if args.restore and (args.distance is not None or args.apply):
        parser.error("--restore cannot be combined with distance or --apply")
    if not args.restore and (args.distance is None or not 0 <= args.distance <= 2147483647):
        parser.error("Specify a distance from 0 to 2147483647")
    root = args.world.resolve()
    if not (root / "level.dat").is_file():
        parser.error(f"Not a world folder (level.dat missing): {root}")
    start = time.monotonic()
    try:
        with world_lock(root):
            if args.restore:
                restore(root, args.restore if args.restore.is_absolute() else root / args.restore)
                return 0
            claims = load_claims(root)
            print(f"Loaded {sum(map(len, claims.values())):,} claims. Buffer: {args.distance} chunks (square, inclusive).")
            print("Dimensions with no claims will be fully pruned.")
            actions, terrain = plan(root, claims, args.distance)
            removed_bytes = sum(a.size - a.kept_size for a in actions)
            print(f"Selected {terrain:,} terrain chunks across {len(actions):,} affected files.")
            print(f"World storage reduction, excluding retained backups: {removed_bytes / 1024**3:.2f} GiB")
            print(f"Whole files: {sum(a.whole for a in actions):,}; files needing compaction: {sum(not a.whole for a in actions):,}")
            if args.apply:
                apply(root, actions, terrain, args.distance)
            else:
                print("PREVIEW ONLY: no chunk files were changed. Add --apply to delete.")
        print(f"Elapsed: {time.monotonic() - start:.1f}s")
        return 0
    except (OSError, ValueError, RuntimeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("Cancelled.", file=sys.stderr)
        return 130


if __name__ == "__main__":
    sys.exit(main())
