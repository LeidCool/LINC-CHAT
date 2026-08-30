package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.integration.SchedulerProvider;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

/**
 * Optional plain-text chat log with daily rotation and retention pruning (TOR section 11).
 * Disabled by default via {@code moderation.chat-log.enabled}.
 */
public final class ChatLogService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Plugin plugin;
    private final SchedulerProvider scheduler;
    private final MainConfig config;
    private final Path logFolder;

    public ChatLogService(Plugin plugin, SchedulerProvider scheduler, MainConfig config) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.config = config;
        this.logFolder = plugin.getDataFolder().toPath().resolve("logs").resolve("chat");
    }

    public void log(String channelId, String senderName, String plainMessage) {
        if (!config.chatLogEnabled()) {
            return;
        }
        String line = "[" + LocalDateTime.now() + "] [" + channelId + "] " + senderName + ": " + plainMessage;
        scheduler.runAsync(() -> writeLine(line));
    }

    private void writeLine(String line) {
        try {
            Files.createDirectories(logFolder);
            Path file = logFolder.resolve(LocalDate.now().format(FILE_DATE) + ".log");
            Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write a chat log line", e);
        }
    }

    /** Deletes rotated log files older than {@code moderation.chat-log.retention-days}. */
    public void pruneOldLogs() {
        int retentionDays = config.chatLogRetentionDays();
        if (retentionDays <= 0) {
            return;
        }
        scheduler.runAsync(() -> {
            if (!Files.isDirectory(logFolder)) {
                return;
            }
            LocalDate cutoff = LocalDate.now().minusDays(retentionDays);
            try (var stream = Files.list(logFolder)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".log")).forEach(path -> {
                    String name = path.getFileName().toString().replace(".log", "");
                    try {
                        LocalDate fileDate = LocalDate.parse(name, FILE_DATE);
                        if (fileDate.isBefore(cutoff)) {
                            Files.deleteIfExists(path);
                        }
                    } catch (Exception ignored) {
                        // not a date-named rotated log file, leave it alone
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to prune old chat logs", e);
            }
        });
    }
}
