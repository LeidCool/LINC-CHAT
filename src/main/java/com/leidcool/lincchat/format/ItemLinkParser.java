package com.leidcool.lincchat.format;

import com.leidcool.lincchat.config.MainConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the {@code *item1}..{@code *item9} hotbar item-link tokens (TOR section 12.6;
 * the token keyword is configurable via {@code item-link.keyword} in {@code config.yml}):
 * a token is replaced by an icon-style component carrying a full {@code HoverEvent.ShowItem}
 * (including enchantments/NBT, via Paper's {@code ItemStack#asHoverEvent()} bridge) built from
 * a snapshot of the hotbar slot taken at send time.
 * <p>
 * Operates directly on the already-coloured message {@link Component} (see {@link
 * ComponentTextReplacer}) rather than on the raw string, so a {@code <gradient>}/{@code <hex>}
 * wrapped around an item token no longer gets torn in half by the token substitution.
 */
public final class ItemLinkParser {

    public sealed interface Outcome {
        record Ok(Component component) implements Outcome {
        }

        record Blocked(String reasonKey, String detail) implements Outcome {
        }
    }

    private ItemLinkParser() {
    }

    public static Pattern pattern(String keyword) {
        return Pattern.compile("(?i)\\*" + Pattern.quote(keyword) + "([1-9])");
    }

    public static Outcome apply(Component input, Player sender, MainConfig config) {
        Pattern tokenPattern = pattern(config.itemLinkKeyword());
        int maxTokens = sender.hasPermission("unichat.bypass.itemlinklimit")
                ? Integer.MAX_VALUE
                : config.itemLinkMaxPerMessage();
        boolean hasPermission = sender.hasPermission("unichat.item.share");
        String behavior = config.itemLinkNoPermissionBehavior();

        // Dry-run over the plain text only: this is purely about deciding whether the whole
        // message must be aborted (block-message / block-if-empty) *before* we start building
        // any replacement components -- nothing here touches styling.
        String plain = PlainTextComponentSerializer.plainText().serialize(input);
        Matcher blockScan = tokenPattern.matcher(plain);
        while (blockScan.find()) {
            if (!hasPermission && "block-message".equals(behavior)) {
                return new Outcome.Blocked("item-link-no-permission", blockScan.group());
            }
            if (hasPermission) {
                int slot = Integer.parseInt(blockScan.group(1)) - 1;
                ItemStack stack = sender.getInventory().getItem(slot);
                boolean empty = stack == null || stack.getType().isAir();
                if (empty && config.itemLinkBlockIfEmpty()) {
                    return new Outcome.Blocked("item-link-blocked-empty", blockScan.group(1));
                }
            }
        }

        int[] used = {0};
        Component result = ComponentTextReplacer.replace(input, tokenPattern, (match, ambientStyle) -> {
            if (!hasPermission) {
                return switch (behavior) {
                    case "strip" -> Component.empty();
                    case "block-message" -> Component.empty(); // unreachable, already blocked above
                    default -> null; // keep-text: leave the token as literal, already-styled text
                };
            }
            if (used[0] >= maxTokens) {
                return null;
            }
            int slot = Integer.parseInt(match.group(1)) - 1;
            ItemStack stack = sender.getInventory().getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                return MiniMessage.miniMessage().deserialize(config.itemLinkEmptySlotText());
            }
            used[0]++;
            return renderIcon(stack.clone(), config.itemLinkDisplayColor(), ambientStyle);
        });
        return new Outcome.Ok(result);
    }

    private static Component renderIcon(ItemStack stack, String displayColorConfig, Style ambientStyle) {
        ItemMeta meta = stack.getItemMeta();
        boolean customName = meta != null && meta.hasDisplayName();
        String name = customName
                ? PlainTextComponentSerializer.plainText().serialize(meta.displayName())
                : prettify(stack.getType().name());

        boolean enchanted = meta != null && (!meta.getEnchants().isEmpty()
                || (meta instanceof EnchantmentStorageMeta esm && !esm.getStoredEnchants().isEmpty()));

        Component text;
        if (displayColorConfig != null && displayColorConfig.equalsIgnoreCase("inherit")) {
            // No explicit colour of its own: takes on whatever colour/gradient was in effect
            // at this exact position in the surrounding message text.
            Component inner = Component.text("[" + name + "]").style(ambientStyle);
            if (customName) {
                inner = inner.decorate(TextDecoration.ITALIC);
            }
            text = inner;
        } else if (displayColorConfig != null && !displayColorConfig.equalsIgnoreCase("auto")) {
            text = MiniMessage.miniMessage().deserialize(displayColorConfig + "[" + name + "]");
        } else {
            NamedTextColor base = enchanted ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.WHITE;
            Component inner = Component.text("[" + name + "]").color(base);
            if (customName) {
                inner = inner.decorate(TextDecoration.ITALIC);
            }
            text = inner;
        }

        return text.hoverEvent(stack.asHoverEvent());
    }

    private static String prettify(String materialName) {
        String[] parts = materialName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
