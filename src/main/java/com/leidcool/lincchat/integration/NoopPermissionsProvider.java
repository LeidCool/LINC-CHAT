package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Fallback used when neither LuckPerms nor Vault Permissions is available. Prefix/suffix
 * simply come back empty and the built-in colour system (permission-node based) still works.
 */
public final class NoopPermissionsProvider implements PermissionsProvider {

    @Override
    public String name() {
        return "none";
    }

    @Override
    public Optional<String> getPrefix(Player player) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSuffix(Player player) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getPrimaryGroupDisplayName(Player player) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMeta(Player player, String key) {
        return Optional.empty();
    }
}
