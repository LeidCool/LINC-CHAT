package com.leidcool.lincchat.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Scans a message {@link Component} for {@code @nick} mentions (TOR section 12.2), bare URLs
 * (TOR section 12.1 -- Minecraft itself asks for confirmation before opening an unclicked link,
 * so no extra safeguard is added here) and, for the Trade channel, price strings (TOR section
 * 6.4) to highlight.
 * <p>
 * Runs via {@link ComponentTextReplacer} on the already-coloured component (see {@link
 * ItemLinkParser}), so a {@code <gradient>}/{@code <hex>} wrapped around a mention/URL/price
 * doesn't get torn in half by the substitution.
 */
public final class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(?<mention>[A-Za-z0-9_]{2,16})");
    private static final Pattern URL_PATTERN =
            Pattern.compile("(?<url>(?:https?://|www\\.)[\\w\\-.~:/?#\\[\\]@!$&'()*+,;=%]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("(?<price>\\d+(?:[.,]\\d+)?\\s*(?:\\$|usd|coins?|монет\\w*))", Pattern.CASE_INSENSITIVE);

    private MentionParser() {
    }

    public record Outcome(Component component, List<Player> mentionedPlayers) {
    }

    /**
     * @param priceColor {@code null} to skip price highlighting entirely (non-Trade channels).
     */
    public static Outcome apply(Component input, String mentionColor, String priceColor) {
        Pattern combined = priceColor == null
                ? Pattern.compile(MENTION_PATTERN.pattern() + "|" + URL_PATTERN.pattern(), Pattern.CASE_INSENSITIVE)
                : Pattern.compile(MENTION_PATTERN.pattern() + "|" + URL_PATTERN.pattern() + "|" + PRICE_PATTERN.pattern(),
                        Pattern.CASE_INSENSITIVE);

        List<Player> mentioned = new ArrayList<>();

        Component result = ComponentTextReplacer.replace(input, combined, (match, ambientStyle) -> {
            String mentionName = safeGroup(match, "mention");
            String url = safeGroup(match, "url");
            String price = priceColor != null ? safeGroup(match, "price") : null;

            if (mentionName != null) {
                Player target = Bukkit.getPlayerExact(mentionName);
                if (target != null && target.isOnline()) {
                    mentioned.add(target);
                    return MiniMessage.miniMessage().deserialize(mentionColor + "@" + target.getName());
                }
                return null;
            }
            if (url != null) {
                return buildLink(url);
            }
            if (price != null) {
                return MiniMessage.miniMessage().deserialize(priceColor + price);
            }
            return null;
        });

        return new Outcome(result, mentioned);
    }

    private static String safeGroup(java.util.regex.Matcher match, String name) {
        try {
            return match.group(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Component buildLink(String url) {
        String target = url.toLowerCase(Locale.ROOT).startsWith("http") ? url : "http://" + url;
        return Component.text(url)
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.BLUE)
                .clickEvent(ClickEvent.openUrl(target));
    }
}
