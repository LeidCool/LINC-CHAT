package com.leidcool.lincchat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts legacy {@code &}-colour codes to MiniMessage tags (TOR section 3/7), so admins used
 * to the old format can keep writing {@code &a} in configs/commands and players' input is
 * still auto-converted before validation.
 * <p>
 * MiniMessage tags already present in the input ({@code <gradient:...>} / {@code <#rrggbb>} /
 * etc.) are lifted out before the legacy round-trip: {@link MiniMessage#serialize(Component)
 * MiniMessage.serialize} would otherwise escape their {@code <}/{@code >} and the tags would
 * stop working when mixed with {@code &}-codes in the same message.
 * <p>
 * LuckPerms / TAB often store a fake two-hex "gradient" as {@code <#rrggbb>text</#rrggbb>}.
 * Standard MiniMessage treats the closing tag as a different colour and leaves
 * {@code </#rrggbb>} as literal text — those pairs are rewritten to a real {@code <gradient>}
 * before parsing.
 */
public final class LegacyColorConverter {

    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("<[^<>]+>");
    /**
     * LuckPerms / EssentialsX / TAB hex "gradient": {@code <#rrggbb>text</#rrggbb>} with two
     * different colours.
     */
    private static final Pattern HEX_PAIR_GRADIENT = Pattern.compile(
            "<#([0-9a-fA-F]{6})>(.*?)</#([0-9a-fA-F]{6})>", Pattern.DOTALL);

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final char PLACEHOLDER_START = '\u0001';
    private static final char PLACEHOLDER_END = '\u0002';

    private LegacyColorConverter() {
    }

    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String normalized = convertHexPairGradients(input);
        if (normalized.indexOf('&') < 0) {
            return normalized;
        }
        if (normalized.indexOf('<') < 0) {
            return serializeLegacy(normalized);
        }

        List<String> tags = new ArrayList<>();
        Matcher matcher = MINIMESSAGE_TAG.matcher(normalized);
        StringBuilder masked = new StringBuilder();
        while (matcher.find()) {
            tags.add(matcher.group());
            matcher.appendReplacement(masked, Matcher.quoteReplacement(
                    PLACEHOLDER_START + String.valueOf(tags.size() - 1) + PLACEHOLDER_END));
        }
        matcher.appendTail(masked);

        String converted = serializeLegacy(masked.toString());
        for (int i = 0; i < tags.size(); i++) {
            converted = converted.replace(PLACEHOLDER_START + String.valueOf(i) + PLACEHOLDER_END, tags.get(i));
        }
        return converted;
    }

    /**
     * Rewrites {@code <#start>inner</#end>} into {@code <gradient:#start:#end>inner</gradient>}
     * when the two hex colours differ. Same-colour pairs are left alone (valid MiniMessage).
     */
    private static String convertHexPairGradients(String input) {
        if (input == null || input.indexOf('<') < 0) {
            return input;
        }
        String current = input;
        for (int pass = 0; pass < 8; pass++) {
            Matcher matcher = HEX_PAIR_GRADIENT.matcher(current);
            StringBuilder rewritten = new StringBuilder();
            boolean any = false;
            while (matcher.find()) {
                String start = matcher.group(1);
                String inner = matcher.group(2);
                String end = matcher.group(3);
                if (start.equalsIgnoreCase(end)) {
                    matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group()));
                    continue;
                }
                any = true;
                String replacement = "<gradient:#" + start + ":#" + end + ">" + inner + "</gradient>";
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
            }
            if (!any) {
                break;
            }
            matcher.appendTail(rewritten);
            String next = rewritten.toString();
            if (next.equals(current)) {
                break;
            }
            current = next;
        }
        return current;
    }

    private static String serializeLegacy(String input) {
        Component component = LEGACY.deserialize(input);
        return MINI_MESSAGE.serialize(component);
    }
}
