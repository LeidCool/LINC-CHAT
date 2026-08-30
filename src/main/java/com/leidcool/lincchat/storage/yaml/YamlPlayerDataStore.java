package com.leidcool.lincchat.storage.yaml;

import com.leidcool.lincchat.integration.SchedulerProvider;
import com.leidcool.lincchat.storage.PlayerDataStore;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Default storage backend: one YAML file per player under {@code playerdata/<uuid>.yml}
 * (TOR section 13).
 */
public final class YamlPlayerDataStore implements PlayerDataStore {

    private final Plugin plugin;
    private final SchedulerProvider scheduler;
    private final Path folder;

    public YamlPlayerDataStore(Plugin plugin, SchedulerProvider scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.folder = plugin.getDataFolder().toPath().resolve("playerdata");
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create the playerdata folder", e);
        }
    }

    private Path fileFor(UUID uuid) {
        return folder.resolve(uuid + ".yml");
    }

    @Override
    public CompletableFuture<PlayerProfileData> load(UUID uniqueId) {
        CompletableFuture<PlayerProfileData> future = new CompletableFuture<>();
        scheduler.runAsync(() -> future.complete(loadSync(uniqueId)));
        return future;
    }

    private PlayerProfileData loadSync(UUID uniqueId) {
        Path file = fileFor(uniqueId);
        PlayerProfileData data = new PlayerProfileData(uniqueId);
        if (!Files.exists(file)) {
            return data;
        }
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file).build();
            CommentedConfigurationNode root = loader.load();

            String active = root.node("active-channel").getString();
            if (active != null) {
                data.activeChannel(active);
            }

            List<String> disabled = root.node("disabled-channels").getList(String.class, List.of());
            for (String id : disabled) {
                data.setListening(id, false);
            }

            data.nameColor(root.node("colors", "name").getString());
            data.prefixColor(root.node("colors", "prefix").getString());
            data.messageColor(root.node("colors", "message").getString());

            long muteExpiry = root.node("mute", "expiry").getLong(0L);
            if (muteExpiry != 0L) {
                data.mute(muteExpiry, root.node("mute", "reason").getString());
            }

            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.node("channel-mutes").childrenMap().entrySet()) {
                String channelId = String.valueOf(entry.getKey());
                long expiry = entry.getValue().node("expiry").getLong(0L);
                String reason = entry.getValue().node("reason").getString();
                if (expiry != 0L) {
                    data.muteChannel(channelId, expiry, reason);
                }
            }

            List<String> ignored = root.node("ignored-players").getList(String.class, List.of());
            for (String uuidString : ignored) {
                try {
                    data.addIgnored(UUID.fromString(uuidString));
                } catch (IllegalArgumentException ignoredEx) {
                    plugin.getLogger().warning("Skipping malformed ignored-player entry for " + uniqueId + ": " + uuidString);
                }
            }

            String lastMessaged = root.node("last-messaged").getString();
            if (lastMessaged != null) {
                try {
                    data.lastMessaged(UUID.fromString(lastMessaged));
                } catch (IllegalArgumentException ignoredEx) {
                    plugin.getLogger().warning("Skipping malformed last-messaged entry for " + uniqueId);
                }
            }

            data.socialSpyEnabled(root.node("social-spy").getBoolean(false));
            data.mentionsEnabled(root.node("mentions-enabled").getBoolean(true));
            data.clearDirty();
            return data;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player data for " + uniqueId, e);
            return data;
        }
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfileData data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                saveSync(data);
                future.complete(null);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save player data for " + data.uniqueId(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void saveSync(PlayerProfileData data) throws IOException, SerializationException {
        Path file = fileFor(data.uniqueId());
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(file).build();
        CommentedConfigurationNode root = loader.createNode();

        if (data.activeChannel() != null) {
            root.node("active-channel").set(data.activeChannel());
        }
        root.node("disabled-channels").setList(String.class, List.copyOf(data.disabledChannels()));

        if (data.nameColor().isPresent()) {
            root.node("colors", "name").set(data.nameColor().get());
        }
        if (data.prefixColor().isPresent()) {
            root.node("colors", "prefix").set(data.prefixColor().get());
        }
        if (data.messageColor().isPresent()) {
            root.node("colors", "message").set(data.messageColor().get());
        }

        root.node("mute", "expiry").set(data.muteExpiryMillis());
        if (data.muteReason().isPresent()) {
            root.node("mute", "reason").set(data.muteReason().get());
        }

        for (Map.Entry<String, PlayerProfileData.MuteEntry> entry : data.channelMutes().entrySet()) {
            root.node("channel-mutes", entry.getKey(), "expiry").set(entry.getValue().expiryMillis());
            if (entry.getValue().reason() != null) {
                root.node("channel-mutes", entry.getKey(), "reason").set(entry.getValue().reason());
            }
        }

        root.node("ignored-players").setList(String.class, data.ignoredPlayers().stream().map(UUID::toString).toList());

        if (data.lastMessaged().isPresent()) {
            root.node("last-messaged").set(data.lastMessaged().get().toString());
        }
        root.node("social-spy").set(data.socialSpyEnabled());
        root.node("mentions-enabled").set(data.mentionsEnabled());

        loader.save(root);
        data.clearDirty();
    }

    @Override
    public void close() {
        // No persistent resources to release for the flat-file backend.
    }
}
