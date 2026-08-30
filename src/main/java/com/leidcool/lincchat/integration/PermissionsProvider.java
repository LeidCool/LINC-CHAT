package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Abstraction over the permission/vault-style plugin currently backing prefixes, suffixes
 * and meta-data. Business logic (channels, colors, moderation) must depend only on this
 * interface, never directly on the LuckPerms or Vault APIs, so that a future Spigot/Folia
 * port only needs a new implementation of this interface.
 */
public interface PermissionsProvider {

    /**
     * @return a short identifier of the backing implementation, for {@code /unichat debug}.
     */
    String name();

    /**
     * @return the raw prefix configured for the player (LuckPerms prefix / Vault chat prefix),
     * empty if none is set.
     */
    Optional<String> getPrefix(Player player);

    /**
     * @return the raw suffix configured for the player, empty if none is set.
     */
    Optional<String> getSuffix(Player player);

    /**
     * @return the display name of the player's primary group, empty if unavailable.
     */
    Optional<String> getPrimaryGroupDisplayName(Player player);

    /**
     * Reads an arbitrary meta key (e.g. {@code unichat-name-color}, {@code unichat-local-radius})
     * from the player's effective meta-data. Returns empty if the backing provider does not
     * support meta or the key is unset.
     */
    Optional<String> getMeta(Player player, String key);
}
