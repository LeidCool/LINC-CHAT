package com.leidcool.lincchat.util;

import com.leidcool.lincchat.storage.PlayerProfileData;

/** Formats mute/slowmode durations for chat messages. */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    public static String format(long millis) {
        if (millis == PlayerProfileData.PERMANENT) {
            return "∞";
        }
        long totalSeconds = Math.max(0, millis / 1000);
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (sb.isEmpty() || seconds > 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }

    public static String formatRemaining(long expiryMillis) {
        if (expiryMillis == PlayerProfileData.PERMANENT) {
            return "∞";
        }
        return format(Math.max(0, expiryMillis - System.currentTimeMillis()));
    }
}
