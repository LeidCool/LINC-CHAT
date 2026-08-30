package com.leidcool.lincchat.integration.vault;

import com.leidcool.lincchat.integration.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Vault Economy integration, used for the optional Trade channel message cost and balance
 * placeholders (TOR section 5.2).
 */
public final class VaultEconomyHook implements EconomyProvider {

    private final Economy economy;

    private VaultEconomyHook(Economy economy) {
        this.economy = economy;
    }

    public static Optional<VaultEconomyHook> tryCreate() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.of(new VaultEconomyHook(provider.getProvider()));
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }
}
