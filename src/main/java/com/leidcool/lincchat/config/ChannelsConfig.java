package com.leidcool.lincchat.config;

import com.leidcool.lincchat.channel.ChannelType;
import com.leidcool.lincchat.util.LegacyColorConverter;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typed accessor over {@code channels.yml}. See {@code src/main/resources/channels.yml} for
 * the canonical layout (TOR section 6.1 / 14.2).
 */
public final class ChannelsConfig {

    private final ConfigurateFile file;
    private Map<String, ChannelDefinition> definitions = new LinkedHashMap<>();

    public ChannelsConfig(ConfigurateFile file) {
        this.file = file;
        parse();
    }

    public void reloadFromDisk() throws IOException {
        file.reload();
        parse();
    }

    private void parse() {
        Map<String, ChannelDefinition> parsed = new LinkedHashMap<>();
        ConfigurationNode channelsNode = file.root().node("channels");
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : channelsNode.childrenMap().entrySet()) {
            String id = String.valueOf(entry.getKey());
            ConfigurationNode n = entry.getValue();
            ChannelType type = ChannelType.fromId(id);

            ChannelDefinition def = new ChannelDefinition(
                    id,
                    n.node("enabled").getBoolean(true),
                    n.node("display-name").getString(id),
                    LegacyColorConverter.toMiniMessage(n.node("tag").getString("")),
                    LegacyColorConverter.toMiniMessage(n.node("tag-outgoing").getString()),
                    LegacyColorConverter.toMiniMessage(n.node("tag-incoming").getString()),
                    LegacyColorConverter.toMiniMessage(n.node("format").getString(
                            "<tag> <prefix_color><prefix></prefix_color> <name_color><player></name_color><gray>:</gray> <message_color><message></message_color>")),
                    n.node("default").getBoolean(false),
                    n.node("range").getInt(0),
                    n.node("vertical-check").getBoolean(true),
                    n.node("cooldown-seconds").getInt(0),
                    readShortcut(n.node("shortcut")),
                    n.node("item-link").getBoolean(false),
                    n.node("permission", "speak").getString(),
                    n.node("permission", "see").getString(),
                    LegacyColorConverter.toMiniMessage(n.node("price-highlight-color").getString()),
                    n.node("message-cost").getDouble(0.0D),
                    n.node("notify-if-empty").getBoolean(false),
                    type
            );
            parsed.put(id, def);
        }
        this.definitions = parsed;
    }

    public Map<String, ChannelDefinition> definitions() {
        return definitions;
    }

    /**
     * YAML {@code shortcut: !} without quotes is a tag, not a string. Fall back to the raw
     * scalar so an unquoted bang still becomes the global prefix.
     */
    private static String readShortcut(ConfigurationNode node) {
        if (node.virtual()) {
            return null;
        }
        String value = node.getString();
        if (value != null && !value.isEmpty()) {
            return value;
        }
        Object raw = node.raw();
        if (raw == null) {
            return null;
        }
        String asString = String.valueOf(raw).trim();
        return asString.isEmpty() ? null : asString;
    }
}
