package com.leidcool.lincchat.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads {@code messages_<locale>.yml} files (TOR section 15) and resolves keys to MiniMessage
 * {@link Component}s. Falls back to English, then to the raw key, if a translation is missing.
 */
public final class MessagesConfig implements MessagesProvider {

    private static final String[] BUNDLED_LOCALES = {"ru", "en"};

    private final Plugin plugin;
    private final MainConfig mainConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, ConfigurateFile> localeFiles = new LinkedHashMap<>();

    public MessagesConfig(Plugin plugin, MainConfig mainConfig) throws IOException {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        for (String locale : BUNDLED_LOCALES) {
            localeFiles.put(locale, ConfigurateFile.loadOrCreate(plugin, "messages_" + locale + ".yml"));
        }
    }

    public void reload() {
        for (ConfigurateFile file : localeFiles.values()) {
            try {
                file.reload();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to reload a messages file", e);
            }
        }
    }

    @Override
    public String defaultLocale() {
        return mainConfig.locale();
    }

    @Override
    public Component get(String locale, String key, TagResolver... resolvers) {
        String template = raw(locale, key);
        try {
            return miniMessage.deserialize(template, resolvers);
        } catch (RuntimeException ex) {
            return Component.text(template);
        }
    }

    private String raw(String locale, String key) {
        String value = rawOrNull(locale, key);
        if (value != null) {
            return value;
        }
        if (!"en".equals(locale)) {
            value = rawOrNull("en", key);
            if (value != null) {
                return value;
            }
        }
        return key;
    }

    private String rawOrNull(String locale, String key) {
        ConfigurateFile file = localeFiles.get(locale);
        if (file == null) {
            return null;
        }
        ConfigurationNode node = file.root().node(key);
        return node.virtual() ? null : node.getString();
    }
}
