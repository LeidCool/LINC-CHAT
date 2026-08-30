package com.leidcool.lincchat.config;

import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thin wrapper around a Configurate {@link YamlConfigurationLoader} that:
 * <ul>
 *     <li>extracts the bundled default resource on first run;</li>
 *     <li>merges any keys that a newer plugin version added into the admin's existing file
 *     without touching values they already set, then re-saves so the new keys are visible;</li>
 *     <li>keeps comments from the on-disk file intact (Configurate's YAML loader tracks
 *     comments on {@link CommentedConfigurationNode}).</li>
 * </ul>
 */
public final class ConfigurateFile {

    private final Plugin plugin;
    private final String resourceName;
    private final Path path;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    private ConfigurateFile(Plugin plugin, String resourceName, Path path, YamlConfigurationLoader loader,
                             CommentedConfigurationNode root) {
        this.plugin = plugin;
        this.resourceName = resourceName;
        this.path = path;
        this.loader = loader;
        this.root = root;
    }

    public static ConfigurateFile loadOrCreate(Plugin plugin, String resourceName) throws IOException {
        Path dataFolder = plugin.getDataFolder().toPath();
        Path target = dataFolder.resolve(resourceName);
        if (!Files.exists(target)) {
            Files.createDirectories(target.getParent());
            try (InputStream in = plugin.getResource(resourceName)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled resource: " + resourceName);
                }
                Files.copy(in, target);
            }
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(target)
                .nodeStyle(NodeStyle.BLOCK)
                .build();

        CommentedConfigurationNode root = loader.load();
        mergeDefaults(plugin, resourceName, root);
        loader.save(root);

        return new ConfigurateFile(plugin, resourceName, target, loader, root);
    }

    private static void mergeDefaults(Plugin plugin, String resourceName, CommentedConfigurationNode root) throws IOException {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in == null) {
                return;
            }
            YamlConfigurationLoader defaultsLoader = YamlConfigurationLoader.builder()
                    .source(() -> new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8)))
                    .build();
            CommentedConfigurationNode defaults = defaultsLoader.load();
            root.mergeFrom(defaults);
        }
    }

    public CommentedConfigurationNode root() {
        return root;
    }

    public void reload() throws IOException {
        this.root = loader.load();
        mergeDefaults(plugin, resourceName, root);
        loader.save(root);
    }

    public void save() throws IOException {
        loader.save(root);
    }

    public Path path() {
        return path;
    }
}
