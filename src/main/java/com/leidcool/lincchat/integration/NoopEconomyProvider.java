package com.leidcool.lincchat.integration;

import org.bukkit.entity.Player;

/**
 * Fallback used when Vault (or an economy plugin behind it) is not installed. All economy
 * gated features (Trade channel message cost, balance placeholder) silently no-op.
 */
public final class NoopEconomyProvider implements EconomyProvider {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public double getBalance(Player player) {
        return 0.0D;
    }

    @Override
    public boolean has(Player player, double amount) {
        return true;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true;
    }

    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}
