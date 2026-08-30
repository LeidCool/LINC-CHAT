package com.leidcool.lincchat.integration.vault;

import com.leidcool.lincchat.integration.PermissionsProvider;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

/**
 * Fallback prefix/suffix source used when LuckPerms is not installed but a legacy
 * permissions plugin (PermissionsEx, GroupManager, ...) is registered through Vault's Chat
 * service (TOR section 5.2). Does not support the plugin's custom meta keys -- those simply
 * come back empty, so colours fall through to the channel/global default.
 */
public final class VaultPermissionHook implements PermissionsProvider {

    private final Chat chat;

    private VaultPermissionHook(Chat chat) {
        this.chat = chat;
    }

    public static Optional<VaultPermissionHook> tryCreate() {
        RegisteredServiceProvider<Chat> provider = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.of(new VaultPermissionHook(provider.getProvider()));
    }

    @Override
    public String name() {
        return "Vault";
    }

    @Override
    public Optional<String> getPrefix(Player player) {
        String prefix = chat.getPlayerPrefix(player);
        return prefix == null || prefix.isEmpty() ? Optional.empty() : Optional.of(prefix);
    }

    @Override
    public Optional<String> getSuffix(Player player) {
        String suffix = chat.getPlayerSuffix(player);
        return suffix == null || suffix.isEmpty() ? Optional.empty() : Optional.of(suffix);
    }

    @Override
    public Optional<String> getPrimaryGroupDisplayName(Player player) {
        String group = chat.getPrimaryGroup(player);
        return group == null || group.isEmpty() ? Optional.empty() : Optional.of(group);
    }

    @Override
    public Optional<String> getMeta(Player player, String key) {
        return Optional.empty();
    }
}
