package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

/**
 * Fallback used when PlaceholderAPI is not installed. Text is returned unchanged; whether
 * the leftover {@code %...%} tokens are stripped afterwards is controlled by
 * {@code config.yml#placeholder-fallback} inside {@code FormatEngine}.
 */
public final class NoopPlaceholderProvider implements PlaceholderProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String parse(Player player, String text) {
        return text;
    }
}
