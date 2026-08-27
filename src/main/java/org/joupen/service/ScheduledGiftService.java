package org.joupen.service;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.joupen.domain.OperationMetadata;
import org.joupen.utils.TimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScheduledGiftService {
    private static final long TICKS_PER_DAY = 24L * 60L * 60L * 20L;

    private final Plugin plugin;
    private final PlayerService playerService;
    private final Path scheduleFile;
    private BukkitTask task;

    public ScheduledGiftService(Plugin plugin, PlayerService playerService, Path scheduleFile) {
        this.plugin = plugin;
        this.playerService = playerService;
        this.scheduleFile = scheduleFile;
    }

    public void start() {
        ensureFileExists();
        long initialDelayTicks = calculateTicksUntilNextMidnight();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::processDueGifts, initialDelayTicks, TICKS_PER_DAY);
        log.info("Scheduled gift checker started. File={}, initialDelayTicks={}", scheduleFile, initialDelayTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void processDueGifts() {
        ensureFileExists();
        LocalDate today = LocalDate.now();
        List<String> remainingLines = new ArrayList<>();

        try {
            for (String line : GiftQueueFile.read(scheduleFile, 3)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                if (parts.length != 4) {
                    log.warn("Invalid scheduled gift format (expected: <nick> <duration> <yyyy-MM-dd>): {}", line);
                    remainingLines.add(line);
                    continue;
                }

                String nick = parts[0];
                String durationRaw = parts[1];
                String dateRaw = parts[2];

                LocalDate executeDate;
                Duration duration;
                try {
                    executeDate = LocalDate.parse(dateRaw);
                    duration = TimeUtils.parseDuration(durationRaw);
                } catch (Exception e) {
                    log.warn("Failed to parse scheduled gift line '{}': {}", line, e.getMessage());
                    remainingLines.add(line);
                    continue;
                }

                if (executeDate.isAfter(today)) {
                    remainingLines.add(line);
                    continue;
                }

                try {
                    playerService.prolongOne(nick, duration, true,
                            new OperationMetadata(dateRaw, "scheduled-gift", null, parts[3]));
                    log.info("Scheduled gift applied for {} on {} (duration={})", nick, today, durationRaw);
                } catch (Exception e) {
                    log.error("Failed to apply scheduled gift for {}: {}", nick, e.getMessage());
                    // Keep the line to retry on next day.
                    remainingLines.add(line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading scheduled gifts file {}: {}", scheduleFile, e.getMessage());
            return;
        }

        try {
            GiftQueueFile.write(scheduleFile, remainingLines);
        } catch (IOException e) {
            log.error("Error writing scheduled gifts file {}: {}", scheduleFile, e.getMessage());
        }
    }

    private void ensureFileExists() {
        try {
            Path parent = scheduleFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(scheduleFile)) {
                Files.createFile(scheduleFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize scheduled gifts file: " + scheduleFile, e);
        }
    }

    private long calculateTicksUntilNextMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secondsUntil = Duration.between(now, nextMidnight).toSeconds();
        return Math.max(20L, secondsUntil * 20L);
    }
}
