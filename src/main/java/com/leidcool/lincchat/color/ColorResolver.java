package com.leidcool.lincchat.color;

import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.integration.PermissionsProvider;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.entity.Player;

/**
 * Implements the colour resolution priority chain from TOR section 8.1:
 * <ol>
 *     <li>the player's own {@code /chatcolor} setting;</li>
 *     <li>the LuckPerms group/user meta keys {@code unichat-prefix-color} /
 *     {@code unichat-name-color} / {@code unichat-message-color};</li>
 *     <li>the server-wide default from {@code config.yml}.</li>
 * </ol>
 * (A channel's own {@code format} template can additionally hard-code a colour by simply not
 * referencing {@code <prefix_color>}/{@code <name_color>}/{@code <message_color>} at all --
 * that is a template authoring choice, not something this class needs to special-case.)
 */
public final class ColorResolver {

    private final PermissionsProvider permissions;
    private final MainConfig config;

    public ColorResolver(PermissionsProvider permissions, MainConfig config) {
        this.permissions = permissions;
        this.config = config;
    }

    public ColorProfile resolve(Player player, PlayerProfileData profile) {
        String prefix = profile.prefixColor()
                .or(() -> permissions.getMeta(player, "unichat-prefix-color"))
                .orElseGet(config::defaultPrefixColor);
        String name = profile.nameColor()
                .or(() -> permissions.getMeta(player, "unichat-name-color"))
                .orElseGet(config::defaultNameColor);
        String message = profile.messageColor()
                .or(() -> permissions.getMeta(player, "unichat-message-color"))
                .orElseGet(config::defaultMessageColor);
        return new ColorProfile(prefix, name, message);
    }
}
