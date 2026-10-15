package dev.ftb.mods.ftbchunks.claimfire;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Offline Anvil pruning. Back up every affected file before changing any of them. */
public final class ClaimChunkPruner {
    public record Claim(int x, int z) { }
    public record Dimension(Path folder, List<Claim> claims) { }
    private record Region(Path file, int[] locations, int[] timestamps, BitSet remove) { }
    private static final Pattern REGION = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final Pattern EXTERNAL = Pattern.compile("c\\.(-?\\d+)\\.(-?\\d+)\\.mcc");

    public static boolean preserve(int x, int z, List<Claim> claims, int distance) {
        for (Claim claim : claims) {
            if (Math.abs((long) x - claim.x) <= distance && Math.abs((long) z - claim.z) <= distance) return true;
        }
        return false;
    }

    public static long prune(Path root, List<Dimension> dimensions, int distance, Path backup) throws IOException {
        if (distance < 0) throw new IllegalArgumentException("Negative distance");
        root = root.toAbsolutePath().normalize();
        List<Region> regions = new ArrayList<>();
        List<Path> external = new ArrayList<>();
        long terrainChunks = 0;
        for (Dimension dimension : dimensions) {
            for (String storage : List.of("region", "entities", "poi")) {
                Path folder = dimension.folder.resolve(storage).toAbsolutePath().normalize();
                if (!folder.startsWith(root)) throw new IOException("Storage outside world: " + folder);
                if (!Files.exists(folder)) continue;
                try (var files = Files.list(folder)) {
                    for (Path file : files.toList()) {
                        var matcher = REGION.matcher(file.getFileName().toString());
                        if (matcher.matches()) {
                            int rx = Integer.parseInt(matcher.group(1)), rz = Integer.parseInt(matcher.group(2));
                            long minX = (long) rx * 32, minZ = (long) rz * 32;
                            var nearby = dimension.claims.stream().filter(claim ->
                                    claim.x >= minX - distance && claim.x <= minX + 31 + distance
                                            && claim.z >= minZ - distance && claim.z <= minZ + 31 + distance).toList();
                            int[] locations = new int[1024], timestamps = new int[1024];
                            BitSet remove = new BitSet(1024);
                            try (RandomAccessFile input = new RandomAccessFile(file.toFile(), "r")) {
                                // Empty placeholders contain no chunks and need no pruning.
                                if (input.length() == 0) continue;
                                if (input.length() < 8192) throw new IOException("Invalid region size: " + file);
                                BitSet occupied = new BitSet();
                                occupied.set(0, 2);
                                for (int i = 0; i < 1024; i++) {
                                    int location = locations[i] = input.readInt();
                                    if (location == 0) continue;
                                    int offset = location >>> 8, sectors = location & 255;
                                    if (offset < 2 || sectors == 0 || (long) offset * 4096 + 5 > input.length()
                                            || occupied.nextSetBit(offset) >= 0 && occupied.nextSetBit(offset) < offset + sectors)
                                        throw new IOException("Invalid region entry: " + file + " #" + i);
                                    long headerPosition = input.getFilePointer();
                                    input.seek((long) offset * 4096);
                                    int payloadLength = input.readInt();
                                    if (payloadLength < 1 || (long) payloadLength + 4 > sectors * 4096L
                                            || (long) offset * 4096 + payloadLength + 4 > input.length())
                                        throw new IOException("Truncated or invalid chunk payload: " + file + " #" + i);
                                    input.seek(headerPosition);
                                    occupied.set(offset, offset + sectors);
                                    int x = Math.toIntExact((long) rx * 32 + i % 32), z = Math.toIntExact((long) rz * 32 + i / 32);
                                    if (!preserve(x, z, nearby, distance)) remove.set(i);
                                }
                                for (int i = 0; i < 1024; i++) timestamps[i] = input.readInt();
                            }
                            if (!remove.isEmpty()) {
                                regions.add(new Region(file, locations, timestamps, remove));
                                if (storage.equals("region")) terrainChunks += remove.cardinality();
                            }
                        } else {
                            var ext = EXTERNAL.matcher(file.getFileName().toString());
                            if (ext.matches() && !preserve(Integer.parseInt(ext.group(1)), Integer.parseInt(ext.group(2)), dimension.claims, distance)) external.add(file);
                        }
                    }
                }
            }
        }
        Files.createDirectories(backup);
        List<Path> affected = new ArrayList<>(regions.stream().map(Region::file).toList());
        affected.addAll(external);
        for (Path file : affected) {
            Path target = backup.resolve(root.relativize(file));
            Files.createDirectories(target.getParent());
            Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
        Files.writeString(backup.resolve("README.txt"), "Chunk pruning backup. Distance: " + distance
                + " chunks (square, inclusive). Terrain chunks removed: " + terrainChunks
                + ". Restore these files into the world while the server is stopped.\n");
        try {
            for (Region region : regions) rewrite(region);
            for (Path file : external) Files.delete(file);
        } catch (IOException | RuntimeException failure) {
            for (Path file : affected) {
                try { Files.copy(backup.resolve(root.relativize(file)), file, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException restore) { failure.addSuppressed(restore); }
            }
            throw failure;
        }
        return terrainChunks;
    }

    private static void rewrite(Region region) throws IOException {
        Path temp = Files.createTempFile(region.file.getParent(), "claim-prune-", ".tmp");
        try {
            int kept = 0;
            try (RandomAccessFile input = new RandomAccessFile(region.file.toFile(), "r");
                 RandomAccessFile output = new RandomAccessFile(temp.toFile(), "rw")) {
                output.setLength(8192);
                int sector = 2;
                for (int i = 0; i < 1024; i++) {
                    int location = region.locations[i];
                    if (location == 0 || region.remove.get(i)) continue;
                    int length = (location & 255) * 4096;
                    byte[] payload = new byte[length];
                    long start = (long) (location >>> 8) * 4096;
                    input.seek(start);
                    // Some writers omit the final sector's unused padding. Payloads were
                    // validated above; restore the missing padding as zeroes when copying.
                    input.readFully(payload, 0, (int) Math.min(length, input.length() - start));
                    output.seek((long) sector * 4096);
                    output.write(payload);
                    output.seek(i * 4L);
                    output.writeInt((sector << 8) | (location & 255));
                    output.seek(4096 + i * 4L);
                    output.writeInt(region.timestamps[i]);
                    sector += location & 255;
                    kept++;
                }
                output.getFD().sync();
            }
            if (kept == 0) Files.delete(region.file);
            else Files.move(temp, region.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
