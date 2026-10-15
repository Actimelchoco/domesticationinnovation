import dev.ftb.mods.ftbchunks.claimfire.ClaimChunkPruner;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ClaimChunkPrunerRegression {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static void region(Path file) throws Exception {
        Files.createDirectories(file.getParent());
        try (RandomAccessFile out = new RandomAccessFile(file.toFile(), "rw")) {
            out.setLength(4 * 4096);
            out.writeInt((2 << 8) | 1);
            out.writeInt((3 << 8) | 1);
            out.seek(4096); out.writeInt(123); out.writeInt(456);
            out.seek(8192); out.writeInt(42);
            out.seek(12288); out.writeInt(99);
            // Valid payload, but the final sector's unused padding was omitted.
            out.setLength(12288 + 103);
        }
    }
    public static void main(String[] args) throws Exception {
        var origin = List.of(new ClaimChunkPruner.Claim(0, 0));
        check(ClaimChunkPruner.preserve(25, -25, origin, 25), "inclusive square boundary");
        check(!ClaimChunkPruner.preserve(26, 0, origin, 25), "outside boundary");
        check(!ClaimChunkPruner.preserve(Integer.MIN_VALUE, 0, List.of(new ClaimChunkPruner.Claim(Integer.MAX_VALUE, 0)), 25), "no overflow");
        Path root = Files.createTempDirectory("claim-pruner-regression-");
        var claim = List.of(new ClaimChunkPruner.Claim(-31, -32));
        var dims = List.of(new ClaimChunkPruner.Dimension(root, claim));
        Path placeholder = root.resolve("region/r.-1.-24.mca");
        Files.createDirectories(placeholder.getParent());
        Files.createFile(placeholder);
        Map<Path, byte[]> originals = new HashMap<>();
        for (String storage : List.of("region", "entities", "poi")) {
            Path file = root.resolve(storage).resolve("r.-1.-1.mca");
            region(file); originals.put(file, Files.readAllBytes(file));
            Files.writeString(file.resolveSibling("c.-32.-32.mcc"), "deleted external payload");
            Files.writeString(file.resolveSibling("c.-31.-32.mcc"), "kept external payload");
        }
        Path backup = root.resolve("backup");
        check(ClaimChunkPruner.prune(root, dims, 0, backup) == 1, "terrain count");
        check(Files.exists(placeholder) && Files.size(placeholder) == 0, "empty placeholder left untouched");
        for (Path file : originals.keySet()) {
            check(Arrays.equals(originals.get(file), Files.readAllBytes(backup.resolve(root.relativize(file)))), "exact backup");
            check(Files.size(file) == 12288, "compacted");
            try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
                check(in.readInt() == 0, "deleted header");
                check(in.readInt() == 513, "relocated survivor");
                in.seek(4100); check(in.readInt() == 456, "timestamp preserved");
                in.seek(8192); check(in.readInt() == 99, "payload preserved");
            }
            check(!Files.exists(file.resolveSibling("c.-32.-32.mcc")), "external deleted");
            check(Files.exists(file.resolveSibling("c.-31.-32.mcc")), "external preserved");
        }
        check(ClaimChunkPruner.prune(root, dims, 0, root.resolve("again")) == 0, "idempotent");
        check(ClaimChunkPruner.prune(root, List.of(new ClaimChunkPruner.Dimension(root, List.of())), 25, root.resolve("empty")) == 1, "no claims deletes remaining");
        check(!Files.exists(root.resolve("region/r.-1.-1.mca")), "empty region removed");
        Path valid = root.resolve("region/r.0.0.mca"), invalid = root.resolve("poi/r.0.0.mca");
        region(valid);
        try (RandomAccessFile out = new RandomAccessFile(valid.toFile(), "rw")) { out.setLength(8192 + 45); }
        byte[] truncated = Files.readAllBytes(valid);
        try {
            ClaimChunkPruner.prune(root, dims, 0, root.resolve("truncated"));
            throw new AssertionError("truncated payload accepted");
        } catch (IOException expected) { }
        check(Arrays.equals(truncated, Files.readAllBytes(valid)), "truncated payload failure changes nothing");
        region(valid); Files.writeString(invalid, "bad header");
        byte[] before = Files.readAllBytes(valid);
        try {
            ClaimChunkPruner.prune(root, dims, 0, root.resolve("invalid"));
            throw new AssertionError("corruption accepted");
        } catch (IOException expected) { }
        check(Arrays.equals(before, Files.readAllBytes(valid)), "preflight failure changes nothing");
        System.out.println("PASS: boundaries, negative coordinates, overflow, terrain/entities/POI, backups, external chunks, compaction, no claims, idempotence, corrupt-file abort. Fixtures: " + root);
    }
}
