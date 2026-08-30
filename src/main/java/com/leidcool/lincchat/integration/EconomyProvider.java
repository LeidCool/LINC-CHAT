package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

/**
 * Abstraction over Vault Economy. All economic features (Trade channel message cost,
 * balance placeholders) go through this interface so the plugin keeps working -- with the
 * feature silently disabled -- when Vault/an economy plugin is not installed.
 */
public interface EconomyProvider {

    boolean isEnabled();

    double getBalance(Player player);

    boolean has(Player player, double amount);

    /**
     * Withdraws the given amount from the player's account.
     *
     * @return {@code true} if the withdrawal succeeded.
     */
    boolean withdraw(Player player, double amount);

    /**
     * Formats an amount using the economy plugin's currency names, e.g. {@code "100 coins"}.
     * Falls back to a plain number when disabled.
     */
    String format(double amount);
}
