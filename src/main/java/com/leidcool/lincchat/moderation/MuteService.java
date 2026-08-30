package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.storage.PlayerProfileData;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global and per-channel mutes with optional TTL (TOR section 11 / 6.1). Bypassed by
 * {@code unichat.bypass.mute}.
 */
public final class MuteService {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([smhd])");

    public record ParsedDuration(boolean permanent, long millis) {
        public static ParsedDuration ofPermanent() {
            return new ParsedDuration(true, 0L);
        }

        public static ParsedDuration of(long millis) {
            return new ParsedDuration(false, millis);
        }

        public long expiryFromNow() {
            return permanent ? PlayerProfileData.PERMANENT : System.currentTimeMillis() + millis;
        }
    }

    /** Parses durations like {@code 10m}, {@code 2h30m}, {@code 1d}, or {@code perm}/{@code permanent}. */
    public static Optional<ParsedDuration> parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("perm") || normalized.equals("permanent") || normalized.equals("-1")) {
            return Optional.of(ParsedDuration.ofPermanent());
        }
        Matcher matcher = DURATION_PART.matcher(normalized);
        long totalMillis = 0;
        boolean any = false;
        while (matcher.find()) {
            any = true;
            long value = Long.parseLong(matcher.group(1));
            totalMillis += switch (matcher.group(2)) {
                case "s" -> value * 1_000L;
                case "m" -> value * 60_000L;
                case "h" -> value * 3_600_000L;
                case "d" -> value * 86_400_000L;
                default -> 0L;
            };
        }
        return any ? Optional.of(ParsedDuration.of(totalMillis)) : Optional.empty();
    }

    public boolean isEffectivelyMuted(PlayerProfileData profile, Channel channel) {
        return profile.isMuted() || profile.isMuted(channel.id());
    }

    public void mute(PlayerProfileData profile, ParsedDuration duration, String reason) {
        profile.mute(duration.expiryFromNow(), reason);
    }

    public void muteChannel(PlayerProfileData profile, String channelId, ParsedDuration duration, String reason) {
        profile.muteChannel(channelId, duration.expiryFromNow(), reason);
    }

    public void unmute(PlayerProfileData profile) {
        profile.unmute();
    }

    public void unmuteChannel(PlayerProfileData profile, String channelId) {
        profile.unmuteChannel(channelId);
    }
}
