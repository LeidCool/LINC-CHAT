package com.leidcool.lincchat.color;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

import java.util.regex.Pattern;

/**
 * Validates user-supplied colour input (TOR section 8.3, last bullet): only colour-related
 * MiniMessage tags are ever accepted -- {@code <click>}, {@code <hover>}, {@code <insert>},
 * NBT/selector tags etc. are never executed, because the underlying {@link MiniMessage}
 * instances are built with a resolver that simply does not know about them. Unknown tags
 * are treated as literal text. Strict mode is intentionally off so player chat can use
 * unclosed colour tags ({@code <#ff8800>text}, {@code &a} / {@code <green>}) and {@code &r}
 * ({@code <reset>}), matching vanilla Minecraft colour-code behaviour.
 */
public final class MiniMessageSanitizer {

    private static final Pattern HEX_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern GRADIENT_OR_RAINBOW_PATTERN = Pattern.compile("(?i)<\\s*/?\\s*(gradient|rainbow)");

    private static final TagResolver COLOR_ONLY_RESOLVER = TagResolver.resolver(
            StandardTags.color(), StandardTags.decorations(), StandardTags.reset());
    private static final TagResolver COLOR_GRADIENT_RESOLVER = TagResolver.resolver(
            StandardTags.color(), StandardTags.decorations(), StandardTags.reset(),
            StandardTags.gradient(), StandardTags.rainbow());

    private static final MiniMessage COLOR_ONLY_PARSER = MiniMessage.builder()
            .tags(COLOR_ONLY_RESOLVER)
            .build();
    private static final MiniMessage GRADIENT_PARSER = MiniMessage.builder()
            .tags(COLOR_GRADIENT_RESOLVER)
            .build();

    private MiniMessageSanitizer() {
    }

    public static final class RejectedException extends RuntimeException {
        public RejectedException(String message) {
            super(message);
        }
    }

    /**
     * Parses {@code rawInput} (legacy {@code &} codes should already be converted via
     * {@code LegacyColorConverter} before calling this) against the given tier and returns
     * a safe coloured {@link Component}.
     *
     * @throws RejectedException if the input uses colour types outside the permitted tier.
     */
    public static Component parse(String rawInput, ColorPermissionPolicy.Tier tier) {
        MiniMessage parser = parserFor(tier, rawInput);
        try {
            return parser.deserialize(rawInput == null ? "" : rawInput);
        } catch (RuntimeException ex) {
            throw new RejectedException("invalid colour tag: " + ex.getMessage());
        }
    }

    /**
     * Validates {@code rawInput} against the given tier and returns a canonical, safe
     * MiniMessage string (used when the colour must be stored, e.g. {@code /chatcolor}).
     *
     * @throws RejectedException if the input uses colour types outside the permitted tier.
     */
    public static String sanitize(String rawInput, ColorPermissionPolicy.Tier tier) {
        MiniMessage parser = parserFor(tier, rawInput);
        return parser.serialize(parse(rawInput, tier));
    }

    private static MiniMessage parserFor(ColorPermissionPolicy.Tier tier, String rawInput) {
        if (tier == ColorPermissionPolicy.Tier.NONE) {
            throw new RejectedException("no colour permission");
        }
        String input = rawInput == null ? "" : rawInput;
        if (tier != ColorPermissionPolicy.Tier.GRADIENT && GRADIENT_OR_RAINBOW_PATTERN.matcher(input).find()) {
            throw new RejectedException("gradient/rainbow tags are not permitted for this player");
        }
        if (tier == ColorPermissionPolicy.Tier.BASIC && HEX_PATTERN.matcher(input).find()) {
            throw new RejectedException("hex colours are not permitted for this player");
        }
        return tier == ColorPermissionPolicy.Tier.GRADIENT ? GRADIENT_PARSER : COLOR_ONLY_PARSER;
    }
}
