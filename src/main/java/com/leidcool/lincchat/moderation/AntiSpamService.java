package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.config.MainConfig;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-channel message cooldowns and "anti-flood" duplicate/near-duplicate detection
 * (TOR section 11). Bypassed by {@code unichat.bypass.cooldown} / {@code unichat.bypass.filter}.
 */
public final class AntiSpamService {

    public enum Verdict {
        ALLOW, COOLDOWN, TOO_SIMILAR
    }

    public record Result(Verdict verdict, long remainingCooldownSeconds) {
        public static final Result ALLOW = new Result(Verdict.ALLOW, 0);
    }

    private final MainConfig config;
    private final Map<UUID, Map<String, Long>> lastMessageTimeByChannel = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();

    public AntiSpamService(MainConfig config) {
        this.config = config;
    }

    public Result check(Player player, Channel channel, String message) {
        boolean bypassCooldown = player.hasPermission("unichat.bypass.cooldown");
        int cooldown = channel.definition().cooldownSeconds();
        if (!bypassCooldown && cooldown > 0) {
            long now = System.currentTimeMillis();
            Map<String, Long> perChannel = lastMessageTimeByChannel.get(player.getUniqueId());
            Long last = perChannel == null ? null : perChannel.get(channel.id());
            if (last != null) {
                long remainingMillis = cooldown * 1000L - (now - last);
                if (remainingMillis > 0) {
                    return new Result(Verdict.COOLDOWN, (remainingMillis + 999) / 1000);
                }
            }
        }

        if (!player.hasPermission("unichat.bypass.filter")) {
            String previous = lastMessageContent.get(player.getUniqueId());
            if (previous != null && similarityPercent(previous, message) >= config.antiSpamSimilarityThreshold()) {
                return new Result(Verdict.TOO_SIMILAR, 0);
            }
        }

        return Result.ALLOW;
    }

    /** Applies an additional server-wide minimum cooldown (e.g. from {@code /slowmode}). */
    public Result checkWithMinimumCooldown(Player player, Channel channel, String message, int minimumCooldownSeconds) {
        if (minimumCooldownSeconds <= 0 || player.hasPermission("unichat.bypass.cooldown")) {
            return check(player, channel, message);
        }
        long now = System.currentTimeMillis();
        Map<String, Long> perChannel = lastMessageTimeByChannel.get(player.getUniqueId());
        Long last = perChannel == null ? null : perChannel.get(channel.id());
        if (last != null) {
            long remainingMillis = minimumCooldownSeconds * 1000L - (now - last);
            if (remainingMillis > 0) {
                return new Result(Verdict.COOLDOWN, (remainingMillis + 999) / 1000);
            }
        }
        return check(player, channel, message);
    }

    public void recordAccepted(Player player, Channel channel, String message) {
        lastMessageTimeByChannel.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(channel.id(), System.currentTimeMillis());
        lastMessageContent.put(player.getUniqueId(), message);
    }

    public void clear(UUID uuid) {
        lastMessageTimeByChannel.remove(uuid);
        lastMessageContent.remove(uuid);
    }

    private static int similarityPercent(String a, String b) {
        String left = a.toLowerCase(Locale.ROOT);
        String right = b.toLowerCase(Locale.ROOT);
        int maxLen = Math.max(left.length(), right.length());
        if (maxLen == 0) {
            return 100;
        }
        int distance = levenshtein(left, right);
        return (int) Math.round((1.0 - (double) distance / maxLen) * 100);
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
