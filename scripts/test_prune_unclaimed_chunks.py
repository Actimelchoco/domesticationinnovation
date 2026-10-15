import contextlib
import importlib.util
import io
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("pruner", Path(__file__).with_name("prune_unclaimed_chunks.py"))
pruner = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = pruner
spec.loader.exec_module(pruner)


def region(path):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = bytearray(12288 + 103)  # Valid final payload with no sector padding.
    struct.pack_into(">II", data, 0, 513, 769)
    struct.pack_into(">II", data, 4096, 123, 456)
    struct.pack_into(">I", data, 8192, 42)
    struct.pack_into(">I", data, 12288, 99)
    data[8196] = data[12292] = 2
    path.write_bytes(data)
    return bytes(data)


class PruningTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        (self.root / "level.dat").touch()
        self.claim_folder = self.root / "ftbchunks"
        self.claim_folder.mkdir()
        self.claim_file = self.claim_folder / "team.snbt"
        self.claim_file.write_text('''{
            chunks: { "minecraft:overworld": [{x:-31,z:-32,time:123L}] }
            member_data: { uuid: { original_claims: { "minecraft:overworld": [{x:-32,z:-32}] } } }
        }''')
        self.output = contextlib.redirect_stdout(io.StringIO())
        self.output.__enter__()
        self.addCleanup(self.output.__exit__, None, None, None)

    def test_claims_and_parser(self):
        claims = pruner.load_claims(self.root)
        self.assertEqual(claims, {"minecraft:overworld": {(-31, -32)}})
        data = pruner.SNBT("{#comment\n foo: [I;1,2] bar: -1b baz: 'quoted' }").read()
        self.assertEqual(data, {"foo": [1, 2], "bar": -1, "baz": "quoted"})
        self.assertTrue(pruner.preserve(25, -25, {(0, 0)}, 25))
        self.assertFalse(pruner.preserve(26, 0, {(0, 0)}, 25))
        self.claim_file.write_text('{chunks:{"minecraft:overworld":[{x:1}]}}')
        with self.assertRaises(ValueError):
            pruner.load_claims(self.root)
        self.claim_file.unlink()
        with self.assertRaises(ValueError):
            pruner.load_claims(self.root)

    def fixture(self):
        originals = {}
        for storage in ("region", "entities", "poi"):
            file = self.root / storage / "r.-1.-1.mca"
            originals[file] = region(file)
            outside = file.with_name("c.-32.-32.mcc")
            outside.write_bytes(b"external delete")
            originals[outside] = outside.read_bytes()
            file.with_name("c.-31.-32.mcc").write_bytes(b"external keep")
        placeholder = self.root / "region/r.-1.-24.mca"
        placeholder.touch()
        whole = self.root / "dimensions/example/nested/moon/region/r.0.0.mca"
        originals[whole] = region(whole)
        return originals, placeholder, whole

    def test_prune_and_restore(self):
        originals, placeholder, whole = self.fixture()
        actions, terrain = pruner.plan(self.root, pruner.load_claims(self.root), 0)
        self.assertEqual(terrain, 3)
        self.assertTrue(whole.exists())  # Preview leaves files untouched.
        backup = pruner.apply(self.root, actions, terrain, 0)
        self.assertFalse(whole.exists())
        self.assertEqual(placeholder.stat().st_size, 0)
        for file, original in originals.items():
            self.assertEqual((backup / file.relative_to(self.root)).read_bytes(), original)
        for storage in ("region", "entities", "poi"):
            file = self.root / storage / "r.-1.-1.mca"
            data = file.read_bytes()
            self.assertEqual(len(data), 12288)
            self.assertEqual(struct.unpack_from(">II", data), (0, 513))
            self.assertEqual(struct.unpack_from(">I", data, 4100)[0], 456)
            self.assertEqual(data[8192:8295], originals[file][12288:12391])
            self.assertFalse(file.with_name("c.-32.-32.mcc").exists())
            self.assertTrue(file.with_name("c.-31.-32.mcc").exists())
        again, terrain = pruner.plan(self.root, pruner.load_claims(self.root), 0)
        self.assertEqual((len(again), terrain), (0, 0))
        pruner.restore(self.root, backup)
        for file, original in originals.items():
            self.assertEqual(file.read_bytes(), original)

    def test_failure_restores_moved_and_partial_files(self):
        originals, _, _ = self.fixture()
        actions, terrain = pruner.plan(self.root, pruner.load_claims(self.root), 0)
        actions.sort(key=lambda a: not a.whole)
        with patch.object(pruner, "compact", side_effect=OSError("simulated disk failure")):
            with self.assertRaises(OSError):
                pruner.apply(self.root, actions, terrain, 0)
        for file, original in originals.items():
            self.assertEqual(file.read_bytes(), original)

    def test_malformed_payload_aborts_preview(self):
        file = self.root / "region/r.0.0.mca"
        region(file)
        with file.open("r+b") as handle:
            handle.truncate(8237)  # First payload declares 46 bytes, only 45 remain.
        before = file.read_bytes()
        with self.assertRaises(ValueError):
            pruner.plan(self.root, pruner.load_claims(self.root), 0)
        self.assertEqual(file.read_bytes(), before)
        self.assertFalse((self.root / "chunk-pruning-backups").exists())

    def test_world_lock_rejects_another_process(self):
        import subprocess
        script = Path(pruner.__file__).resolve()
        with pruner.world_lock(self.root):
            result = subprocess.run([sys.executable, str(script), "0", "--world", str(self.root)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertIn("World is locked", result.stderr)


if __name__ == "__main__":
    unittest.main()
