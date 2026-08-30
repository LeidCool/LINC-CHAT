package com.leidcool.lincchat.color;

import org.bukkit.entity.Player;

/**
 * Grades a player's access to colour input types (TOR section 8.3): the 16 standard colours,
 * arbitrary HEX, or MiniMessage gradients/rainbow. Each tier implies the previous one.
 */
public final class ColorPermissionPolicy {

    public enum Tier {
        NONE, BASIC, HEX, GRADIENT
    }

    public Tier tierFor(Player player) {
        if (player.hasPermission("unichat.color.gradient")) {
            return Tier.GRADIENT;
        }
        if (player.hasPermission("unichat.color.hex")) {
            return Tier.HEX;
        }
        if (player.hasPermission("unichat.color.basic")) {
            return Tier.BASIC;
        }
        return Tier.NONE;
    }

    public boolean canUseSelfColor(Player player) {
        return player.hasPermission("unichat.color.self");
    }
}
