package com.leidcool.lincchat.format;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.impl.TradeChannel;
import com.leidcool.lincchat.color.ColorPermissionPolicy;
import com.leidcool.lincchat.color.ColorProfile;
import com.leidcool.lincchat.color.MiniMessageSanitizer;
import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.integration.EconomyProvider;
import com.leidcool.lincchat.integration.PermissionsProvider;
import com.leidcool.lincchat.integration.PlaceholderProvider;
import com.leidcool.lincchat.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Assembles the final {@link Component} for a channel message from the raw text plus the
 * sender's colours (TOR sections 4/7/8/12). Colouring is applied directly while building the
 * {@code prefix}/{@code player}/{@code message} sub-components (this also lets a gradient
 * colour interpolate correctly across the actual nickname text); the {@code prefix_color} /
 * {@code name_color} / {@code message_color} tags referenced by {@code channels.yml} templates
 * are still registered as pass-through (no-op) styling tags purely so existing templates using
 * the TOR-documented {@code <name_color>...</name_color>} syntax keep parsing successfully.
 */
public final class FormatEngine {

    private static final Pattern LEFTOVER_PLACEHOLDER_PATTERN = Pattern.compile("%[\\w-]+%");

    private final MainConfig config;
    private final PermissionsProvider permissions;
    private final EconomyProvider economy;
    private final PlaceholderProvider placeholders;
    private final ColorPermissionPolicy colorPolicy;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public FormatEngine(MainConfig config, PermissionsProvider permissions, EconomyProvider economy,
                         PlaceholderProvider placeholders, ColorPermissionPolicy colorPolicy) {
        this.config = config;
        this.permissions = permissions;
        this.economy = economy;
        this.placeholders = placeholders;
        this.colorPolicy = colorPolicy;
    }

    public record RenderResult(Component component, List<Player> mentionedPlayers, boolean blocked,
                                String blockReasonKey, String blockDetail) {
        public static RenderResult blocked(String reasonKey, String detail) {
            return new RenderResult(null, List.of(), true, reasonKey, detail);
        }

        public static RenderResult ok(Component component, List<Player> mentioned) {
            return new RenderResult(component, mentioned, false, null, null);
        }
    }

    public RenderResult renderChannelMessage(Player sender, Channel channel, ColorProfile colors, String rawMessage) {
        ColorPermissionPolicy.Tier messageColorTier = config.allowColorCodesInMessages()
                ? colorPolicy.tierFor(sender)
                : ColorPermissionPolicy.Tier.NONE;
        // Coloured once, as a whole, *before* item-link/mention tokens are located and spliced
        // in -- this is what lets a <gradient>/<hex> wrapped around a *item token or @mention
        // survive intact instead of being torn into two independently-parsed halves.
        Component colored = colorizeWholeMessage(rawMessage, messageColorTier);

        if (channel.definition().itemLinkEnabled() && config.itemLinkEnabled()) {
            ItemLinkParser.Outcome outcome = ItemLinkParser.apply(colored, sender, config);
            if (outcome instanceof ItemLinkParser.Outcome.Blocked blocked) {
                return RenderResult.blocked(blocked.reasonKey(), blocked.detail());
            }
            colored = ((ItemLinkParser.Outcome.Ok) outcome).component();
        }

        String priceColor = channel instanceof TradeChannel trade ? trade.priceHighlightColor() : null;
        String mentionColor = config.mentionsEnabled() ? config.mentionsColor() : "<white>";
        MentionParser.Outcome mentionOutcome = MentionParser.apply(colored, mentionColor, priceColor);

        Component messageComponent = applyBaseColor(mentionOutcome.component(), colors.messageColor());

        Component nameComponent = decorateNameWithHoverAndClick(sender, colors.nameColor());

        String rawPrefix = permissions.getPrefix(sender).orElse("");
        Component prefixComponent = rawPrefix.isBlank()
                ? Component.empty()
                : renderPrefix(rawPrefix, colors.prefixColor());

        String rawBadge = permissions.getMeta(sender, "unichat-badge").orElse("");
        Component badgeComponent = rawBadge.isBlank()
                ? Component.empty()
                : safeDeserialize(LegacyColorConverter.toMiniMessage(rawBadge)).append(Component.text(" "));

        String template = expandThirdPartyPlaceholders(sender, channel.definition().format());

        TagResolver resolver = TagResolver.builder()
                .tag("tag", Tag.inserting(safeDeserialize(channel.tag())))
                .tag("prefix_color", Tag.styling())
                .tag("name_color", Tag.styling())
                .tag("message_color", Tag.styling())
                .resolver(Placeholder.component("prefix", prefixComponent))
                .resolver(Placeholder.component("player", nameComponent))
                .resolver(Placeholder.component("message", messageComponent))
                .resolver(Placeholder.component("badge", badgeComponent))
                .build();

        Component result = miniMessage.deserialize(template, resolver);
        return RenderResult.ok(result, mentionOutcome.mentionedPlayers());
    }

    /**
     * Renders the raw message text typed by the player into a display {@link Component},
     * optionally interpreting colour/format markup (as opposed to the {@code /chatcolor}
     * name/prefix/message layers): legacy {@code &}-codes are always converted first, then the
     * result is parsed <em>as a whole</em> against the player's {@link
     * ColorPermissionPolicy.Tier} via {@link MiniMessageSanitizer} -- so a BASIC-tier player can
     * use {@code &a}/{@code <green>} but not {@code <#ff0055>} or {@code <gradient:...>}, and a
     * player with no colour permission at all gets their message shown as plain, unstyled text.
     * Unclosed colour tags and {@code &r}/{@code <reset>} are accepted (vanilla chat style).
     */
    private Component colorizeWholeMessage(String rawMessage, ColorPermissionPolicy.Tier tier) {
        if (tier == ColorPermissionPolicy.Tier.NONE) {
            return Component.text(rawMessage);
        }
        String converted = LegacyColorConverter.toMiniMessage(rawMessage);
        if (converted.indexOf('<') < 0) {
            return Component.text(converted);
        }
        try {
            return MiniMessageSanitizer.parse(converted, tier);
        } catch (MiniMessageSanitizer.RejectedException ex) {
            return Component.text(rawMessage);
        }
    }

    public Component renderMe(Player sender, String action) {
        return Component.text("* ", NamedTextColor.GRAY)
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text(" " + action, NamedTextColor.GRAY));
    }

    private Component decorateNameWithHoverAndClick(Player player, String nameColor) {
        Component styledName = safeDeserialize(nameColor + player.getName());

        List<Component> hoverLines = new ArrayList<>();
        String rank = permissions.getPrimaryGroupDisplayName(player).orElse("default");
        hoverLines.add(Component.text("Rank: ", NamedTextColor.GRAY).append(Component.text(rank, NamedTextColor.WHITE)));

        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalMinutes = ticks / 1200L;
        hoverLines.add(Component.text("Online: ", NamedTextColor.GRAY)
                .append(Component.text((totalMinutes / 60) + "h " + (totalMinutes % 60) + "m", NamedTextColor.WHITE)));

        if (economy.isEnabled()) {
            hoverLines.add(Component.text("Balance: ", NamedTextColor.GRAY)
                    .append(Component.text(economy.format(economy.getBalance(player)), NamedTextColor.WHITE)));
        }

        Component hover = Component.join(JoinConfiguration.newlines(), hoverLines);

        return styledName.hoverEvent(hover.asHoverEvent())
                .clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " "));
    }

    private Component applyBaseColor(Component content, String colorTag) {
        if (colorTag == null || colorTag.isBlank()) {
            return content;
        }
        Component probe = safeDeserialize(colorTag + "x");
        return probe.color() != null ? content.colorIfAbsent(probe.color()) : content;
    }

    private String expandThirdPartyPlaceholders(Player player, String text) {
        String expanded = placeholders.parse(player, text);
        if ("strip".equalsIgnoreCase(config.placeholderFallback())) {
            expanded = LEFTOVER_PLACEHOLDER_PATTERN.matcher(expanded).replaceAll("");
        }
        return expanded;
    }

    /**
     * LuckPerms prefixes are often already coloured ({@code <#rrggbb>…</#rrggbb>} or a real
     * MiniMessage gradient). Wrapping those with the layer {@code prefix-color} would only
     * add a parent colour and can hide the intended gradient — skip the wrap when the prefix
     * already contains markup.
     */
    private Component renderPrefix(String rawPrefix, String prefixColor) {
        String converted = LegacyColorConverter.toMiniMessage(rawPrefix);
        boolean selfColored = converted.indexOf('<') >= 0 || converted.indexOf('&') >= 0;
        String input = selfColored || prefixColor == null || prefixColor.isBlank()
                ? converted
                : prefixColor + converted;
        return safeDeserialize(input);
    }

    private Component safeDeserialize(String text) {
        try {
            return miniMessage.deserialize(LegacyColorConverter.toMiniMessage(text == null ? "" : text));
        } catch (RuntimeException ex) {
            return Component.text(text == null ? "" : text);
        }
    }
}
