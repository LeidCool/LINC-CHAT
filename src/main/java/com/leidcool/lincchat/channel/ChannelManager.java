package com.leidcool.lincchat.channel;

import com.leidcool.lincchat.channel.impl.AdminChannel;
import com.leidcool.lincchat.channel.impl.CustomChannel;
import com.leidcool.lincchat.channel.impl.GlobalChannel;
import com.leidcool.lincchat.channel.impl.LocalChannel;
import com.leidcool.lincchat.channel.impl.PrivateMessageChannel;
import com.leidcool.lincchat.channel.impl.TradeChannel;
import com.leidcool.lincchat.config.ChannelDefinition;
import com.leidcool.lincchat.config.ChannelsConfig;
import com.leidcool.lincchat.integration.ChannelAccessGuard;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the set of configured channels, (re)builds them from {@link ChannelsConfig} and
 * exposes lookups used by commands/listeners (TOR section 4/6).
 */
public final class ChannelManager {

    private final Plugin plugin;
    private final ChannelContext context;
    private Map<String, Channel> channels = new LinkedHashMap<>();
    private ChannelAccessGuard accessGuard = ChannelAccessGuard.ALLOW_ALL;

    public ChannelManager(Plugin plugin, ChannelContext context) {
        this.plugin = plugin;
        this.context = context;
    }

    public void setAccessGuard(ChannelAccessGuard guard) {
        this.accessGuard = guard == null ? ChannelAccessGuard.ALLOW_ALL : guard;
    }

    public void load(ChannelsConfig config) {
        Map<String, Channel> built = new LinkedHashMap<>();
        for (ChannelDefinition definition : config.definitions().values()) {
            built.put(definition.id(), build(definition));
        }
        this.channels = built;
        registerPermissions();
    }

    private Channel build(ChannelDefinition definition) {
        return switch (definition.type()) {
            case GLOBAL -> new GlobalChannel(definition);
            case LOCAL -> new LocalChannel(definition);
            case TRADE -> new TradeChannel(definition);
            case ADMIN -> new AdminChannel(definition);
            case PRIVATE -> new PrivateMessageChannel(definition);
            case CUSTOM -> new CustomChannel(definition);
        };
    }

    private void registerPermissions() {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (Channel channel : channels.values()) {
            registerIfAbsent(pm, "unichat.channel." + channel.id() + ".speak");
            registerIfAbsent(pm, "unichat.channel." + channel.id() + ".see");
        }
    }

    private void registerIfAbsent(PluginManager pm, String node) {
        if (pm.getPermission(node) == null) {
            pm.addPermission(new Permission(node, PermissionDefault.TRUE));
        }
    }

    public Optional<Channel> get(String id) {
        return Optional.ofNullable(channels.get(id));
    }

    public Collection<Channel> all() {
        return channels.values();
    }

    public List<Channel> enabled() {
        return channels.values().stream().filter(Channel::isEnabled).toList();
    }

    /** Channels a given player may currently switch into / speak in (used for {@code /ch} tab-complete). */
    public List<Channel> usableBy(Player player) {
        return channels.values().stream()
                .filter(Channel::isEnabled)
                .filter(c -> c.type() != ChannelType.PRIVATE)
                .filter(c -> canUse(player, c, true))
                .toList();
    }

    public Optional<Channel> defaultChannel() {
        return channels.values().stream()
                .filter(Channel::isEnabled)
                .filter(c -> c.definition().defaultChannel())
                .findFirst()
                .or(() -> channels.values().stream()
                        .filter(Channel::isEnabled)
                        .filter(c -> c.type() != ChannelType.PRIVATE)
                        .findFirst());
    }

    public boolean canUse(Player player, Channel channel, boolean speak) {
        boolean permissionOk = speak ? channel.canSpeak(player) : channel.canSee(player);
        return permissionOk && accessGuard.canUseChannel(player, channel);
    }

    public ChannelContext context() {
        return context;
    }

    /**
     * Finds a channel whose configured shortcut prefixes the given raw message. When several
     * shortcuts match, the longest one wins ({@code !!} must not lose to {@code !}).
     */
    public Optional<ShortcutMatch> matchShortcut(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Optional.empty();
        }
        Channel best = null;
        String bestShortcut = "";
        for (Channel channel : channels.values()) {
            String shortcut = channel.definition().shortcut();
            if (shortcut == null || shortcut.isEmpty()) {
                continue;
            }
            if (rawMessage.startsWith(shortcut) && shortcut.length() > bestShortcut.length()) {
                best = channel;
                bestShortcut = shortcut;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        String remainder = rawMessage.substring(bestShortcut.length());
        if (remainder.startsWith(" ")) {
            remainder = remainder.substring(1);
        }
        return Optional.of(new ShortcutMatch(best, remainder));
    }

    public record ShortcutMatch(Channel channel, String remainder) {
    }
}
