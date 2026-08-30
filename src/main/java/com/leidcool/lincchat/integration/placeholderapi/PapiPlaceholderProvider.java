package com.leidcool.lincchat.integration.placeholderapi;

import com.leidcool.lincchat.integration.PlaceholderProvider;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Expands third-party PlaceholderAPI placeholders left inside a channel's {@code format}
 * template (TOR section 5.3), e.g. {@code %vault_eco_balance%}, {@code %luckperms_prefix%}.
 */
public final class PapiPlaceholderProvider implements PlaceholderProvider {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String parse(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}
