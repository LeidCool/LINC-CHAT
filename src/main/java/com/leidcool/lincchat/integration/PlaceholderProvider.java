package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

/**
 * Abstraction over PlaceholderAPI, used by {@code FormatEngine} to expand third-party
 * placeholders (e.g. {@code %vault_eco_balance%}, {@code %luckperms_prefix%}) inside
 * channel format templates without a hard dependency on PlaceholderAPI.
 */
public interface PlaceholderProvider {

    boolean isEnabled();

    /**
     * Expands any {@code %placeholder%} tokens in {@code text} for {@code player}.
     * When disabled, behaviour is controlled by the caller (strip/keep), this method
     * simply returns {@code text} unchanged.
     */
    String parse(Player player, String text);
}
