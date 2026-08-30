package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.config.MainConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Optional, disabled-by-default swear-word filter (TOR section 11). Word list supports a
 * simple {@code *} wildcard (e.g. {@code "badword*"}).
 */
public final class SwearFilterService {

    public enum Verdict {
        OK, REPLACED, BLOCKED
    }

    public record Result(Verdict verdict, String text) {
        public static Result ok(String text) {
            return new Result(Verdict.OK, text);
        }
    }

    private final MainConfig config;
    private List<Pattern> wordPatterns = List.of();

    public SwearFilterService(MainConfig config) {
        this.config = config;
        reload();
    }

    public void reload() {
        List<Pattern> patterns = new ArrayList<>();
        for (String word : config.swearFilterWords()) {
            patterns.add(Pattern.compile("(?i)\\b" + wildcardToRegex(word) + "\\b"));
        }
        this.wordPatterns = patterns;
    }

    public Result check(String message, boolean bypass) {
        if (bypass || !config.swearFilterEnabled() || wordPatterns.isEmpty()) {
            return Result.ok(message);
        }

        boolean matchedAny = wordPatterns.stream().anyMatch(pattern -> pattern.matcher(message).find());
        if (!matchedAny) {
            return Result.ok(message);
        }

        String action = config.swearFilterAction().toLowerCase(Locale.ROOT);
        if ("block".equals(action)) {
            return new Result(Verdict.BLOCKED, message);
        }
        if ("warn".equals(action)) {
            return Result.ok(message);
        }

        String text = message;
        for (Pattern pattern : wordPatterns) {
            text = pattern.matcher(text).replaceAll(match -> "*".repeat(match.group().length()));
        }
        return new Result(Verdict.REPLACED, text);
    }

    private static String wildcardToRegex(String pattern) {
        StringBuilder sb = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                sb.append(".*");
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return sb.toString();
    }
}
