"""Port the existing Saints Dragons claim patch only when its original class is unchanged."""
import argparse
import hashlib
from pathlib import Path
from zipfile import ZipFile

TARGET = "com/leon/saintsdragons/server/entity/dragons/util/DragonGriefingRules.class"


def port(original_old, patched_old, original_new, output):
    paths = [Path(p).resolve() for p in (original_old, patched_old, original_new, output)]
    if paths[3] in paths[:3] or paths[3].exists():
        raise ValueError("Output must be a new file, separate from the input jars")
    with ZipFile(paths[0]) as old, ZipFile(paths[1]) as patched, ZipFile(paths[2]) as new:
        if set(old.namelist()) != set(patched.namelist()):
            raise ValueError("Old patch adds or removes entries; review it before porting")
        changed = {n for n in old.namelist() if old.read(n) != patched.read(n)}
        if changed != {TARGET}:
            raise ValueError(f"Unexpected old patch changes: {changed}")
        if old.read(TARGET) != new.read(TARGET):
            raise ValueError("New version changes the target class; a direct port is unsafe")
        if len(new.namelist()) != len(set(new.namelist())):
            raise ValueError("New jar has duplicate entries")
        replacement = patched.read(TARGET)
        paths[3].parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(paths[3], "w") as result:
            result.comment = new.comment
            for entry in new.infolist():
                result.writestr(entry, replacement if entry.filename == TARGET else new.read(entry.filename))
    with ZipFile(paths[2]) as new, ZipFile(paths[3]) as result:
        assert result.testzip() is None, "Corrupt output jar"
        assert new.namelist() == result.namelist(), "Output entries changed"
        changed = [n for n in new.namelist() if new.read(n) != result.read(n)]
        assert changed == [TARGET], changed
        assert result.read(TARGET) == replacement
        print(f"Verified {len(result.namelist())} entries; only {TARGET} changed")
    print(f"Output: {paths[3]}")
    print(f"SHA256: {hashlib.sha256(paths[3].read_bytes()).hexdigest()}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("original_old", "patched_old", "original_new", "output"):
        parser.add_argument(name)
    args = parser.parse_args()
    port(args.original_old, args.patched_old, args.original_new, args.output)
