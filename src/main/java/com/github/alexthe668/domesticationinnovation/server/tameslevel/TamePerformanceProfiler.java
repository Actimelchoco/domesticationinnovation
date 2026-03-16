package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class TamePerformanceProfiler {
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Map<String, Stat> STATS = new ConcurrentHashMap<>();
    private static volatile boolean enabled = false;

    private TamePerformanceProfiler() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void start() {
        enabled = true;
    }

    public static void stop() {
        enabled = false;
    }

    public static void reset() {
        STATS.clear();
    }

    public static void run(String key, Runnable action) {
        if (!enabled || key == null || key.isBlank()) {
            action.run();
            return;
        }
        long start = System.nanoTime();
        try {
            action.run();
        } finally {
            long elapsed = System.nanoTime() - start;
            STATS.computeIfAbsent(key, ignored -> new Stat()).record(elapsed);
        }
    }

    public static Path writeReport() throws IOException {
        Path dir = Path.of("profiling");
        Files.createDirectories(dir);
        Path file = dir.resolve("tameslevel-profile-" + FILE_STAMP.format(LocalDateTime.now()) + ".csv");

        List<Map.Entry<String, Stat>> rows = new ArrayList<>(STATS.entrySet());
        rows.sort(Comparator.comparingLong((Map.Entry<String, Stat> entry) -> entry.getValue().totalNanos.get()).reversed());

        List<String> lines = new ArrayList<>();
        lines.add("key,total_seconds,total_ms,avg_ms,max_ms,count");
        for (Map.Entry<String, Stat> row : rows) {
            Stat stat = row.getValue();
            long totalNanos = stat.totalNanos.get();
            long count = stat.count.get();
            long maxNanos = stat.maxNanos.get();
            double totalMs = totalNanos / 1_000_000.0D;
            double totalSeconds = totalNanos / 1_000_000_000.0D;
            double avgMs = count == 0L ? 0.0D : totalMs / count;
            double maxMs = maxNanos / 1_000_000.0D;
            lines.add(String.format(
                    Locale.ROOT,
                    "%s,%.6f,%.3f,%.6f,%.6f,%d",
                    csv(row.getKey()),
                    totalSeconds,
                    totalMs,
                    avgMs,
                    maxMs,
                    count
            ));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file;
    }

    private static String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static final class Stat {
        private final AtomicLong totalNanos = new AtomicLong();
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong maxNanos = new AtomicLong();

        private void record(long nanos) {
            totalNanos.addAndGet(nanos);
            count.incrementAndGet();
            maxNanos.accumulateAndGet(nanos, Math::max);
        }
    }
}
