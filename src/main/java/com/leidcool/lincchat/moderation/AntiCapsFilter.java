package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.config.MainConfig;

import java.util.Locale;

/**
 * Lower-cases messages that are mostly capital letters (TOR section 11). Stateless, so it is
 * a plain static utility rather than an injected service.
 */
public final class AntiCapsFilter {

    private AntiCapsFilter() {
    }

    public record Result(String text, boolean modified) {
    }

    public static Result process(String message, MainConfig config, boolean bypass) {
        if (bypass || !config.antiCapsEnabled() || message.length() < config.antiCapsMinLength()) {
            return new Result(message, false);
        }
        long letters = message.chars().filter(Character::isLetter).count();
        if (letters == 0) {
            return new Result(message, false);
        }
        long upper = message.chars().filter(Character::isUpperCase).count();
        int percent = (int) Math.round(upper * 100.0 / letters);
        if (percent >= config.antiCapsThresholdPercent()) {
            return new Result(message.toLowerCase(Locale.ROOT), true);
        }
        return new Result(message, false);
    }
}
