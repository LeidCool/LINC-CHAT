package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.config.MainConfig;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional, disabled-by-default filter for links/IPs/Discord invites (TOR section 11).
 * Stateless static utility.
 */
public final class AdvertisingFilter {

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "(?i)\\b(?:(?:[a-z0-9-]+\\.)+(?:com|net|org|ru|io|gg|xyz|info|biz|me|tv|co)(?:/[\\w\\-./?%&=]*)?" +
                    "|\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?|discord\\.gg/\\S+)\\b");

    private AdvertisingFilter() {
    }

    public enum Verdict {
        OK, WARN, REPLACE, BLOCK
    }

    public record Result(Verdict verdict, String text) {
        public static Result ok(String text) {
            return new Result(Verdict.OK, text);
        }
    }

    public static Result check(String message, MainConfig config, boolean bypass) {
        if (bypass || !config.advertisingFilterEnabled()) {
            return Result.ok(message);
        }
        Matcher matcher = LINK_PATTERN.matcher(message);
        if (!matcher.find()) {
            return Result.ok(message);
        }
        String matchedLower = matcher.group().toLowerCase(Locale.ROOT);
        for (String whitelisted : config.advertisingWhitelistDomains()) {
            if (matchedLower.contains(whitelisted.toLowerCase(Locale.ROOT))) {
                return Result.ok(message);
            }
        }
        return switch (config.advertisingFilterAction().toLowerCase(Locale.ROOT)) {
            case "block" -> new Result(Verdict.BLOCK, message);
            case "replace" -> new Result(Verdict.REPLACE, matcher.replaceAll("***"));
            default -> new Result(Verdict.WARN, message);
        };
    }
}
