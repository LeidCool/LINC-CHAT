package com.leidcool.lincchat.config;

import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Typed accessor over {@code config.yml}. See {@code src/main/resources/config.yml} for the
 * canonical layout (TOR section 14.1).
 */
public final class MainConfig {

    private final ConfigurateFile file;

    public MainConfig(ConfigurateFile file) {
        this.file = file;
    }

    private ConfigurationNode node(Object... path) {
        return file.root().node(path);
    }

    public String locale() {
        return node("locale").getString("ru");
    }

    public enum TriState { AUTO, TRUE, FALSE }

    private TriState triState(String value) {
        if (value == null) {
            return TriState.AUTO;
        }
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> TriState.TRUE;
            case "false" -> TriState.FALSE;
            default -> TriState.AUTO;
        };
    }

    /** Resolves an {@code auto|true|false} integration switch against whether the plugin is installed. */
    public boolean integrationEnabled(String key, BooleanSupplier pluginPresent) {
        TriState state = triState(node("integrations", key).getString("auto"));
        return switch (state) {
            case TRUE -> true;
            case FALSE -> false;
            case AUTO -> pluginPresent.getAsBoolean();
        };
    }

    public String storageType() {
        return node("storage", "type").getString("yaml");
    }

    public String placeholderFallback() {
        return node("placeholder-fallback").getString("strip");
    }

    public String defaultPrefixColor() {
        return node("colors", "default-prefix-color").getString("<gray>");
    }

    public String defaultNameColor() {
        return node("colors", "default-name-color").getString("<white>");
    }

    public String defaultMessageColor() {
        return node("colors", "default-message-color").getString("<white>");
    }

    public boolean allowLegacyColorCodesInput() {
        return node("colors", "allow-legacy-color-codes-input").getBoolean(true);
    }

    public boolean allowColorCodesInMessages() {
        return node("colors", "allow-color-codes-in-messages").getBoolean(true);
    }

    public boolean mentionsEnabled() {
        return node("mentions", "enabled").getBoolean(true);
    }

    public String mentionsSound() {
        return node("mentions", "sound").getString("entity.experience_orb.pickup");
    }

    public float mentionsSoundVolume() {
        return (float) node("mentions", "sound-volume").getDouble(1.0D);
    }

    public float mentionsSoundPitch() {
        return (float) node("mentions", "sound-pitch").getDouble(1.0D);
    }

    public String mentionsColor() {
        return node("mentions", "color").getString("<yellow>");
    }

    public boolean itemLinkEnabled() {
        return node("item-link", "enabled").getBoolean(true);
    }

    public String itemLinkKeyword() {
        return node("item-link", "keyword").getString("sale");
    }

    public int itemLinkMaxPerMessage() {
        return node("item-link", "max-per-message").getInt(5);
    }

    public String itemLinkNoPermissionBehavior() {
        return node("item-link", "no-permission-behavior").getString("keep-text");
    }

    public String itemLinkEmptySlotText() {
        return node("item-link", "empty-slot-text").getString("<gray><пусто></gray>");
    }

    public boolean itemLinkBlockIfEmpty() {
        return node("item-link", "block-if-empty").getBoolean(false);
    }

    public String itemLinkDisplayColor() {
        return node("item-link", "display-color").getString("auto");
    }

    public boolean antiCapsEnabled() {
        return node("moderation", "anti-caps", "enabled").getBoolean(true);
    }

    public int antiCapsThresholdPercent() {
        return node("moderation", "anti-caps", "threshold-percent").getInt(70);
    }

    public int antiCapsMinLength() {
        return node("moderation", "anti-caps", "min-length").getInt(6);
    }

    public int antiSpamSimilarityThreshold() {
        return node("moderation", "anti-spam", "similarity-threshold-percent").getInt(85);
    }

    public boolean swearFilterEnabled() {
        return node("moderation", "swear-filter", "enabled").getBoolean(false);
    }

    public String swearFilterAction() {
        return node("moderation", "swear-filter", "action").getString("replace");
    }

    public List<String> swearFilterWords() {
        try {
            return node("moderation", "swear-filter", "words").getList(String.class, List.of());
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            return List.of();
        }
    }

    public boolean advertisingFilterEnabled() {
        return node("moderation", "advertising-filter", "enabled").getBoolean(false);
    }

    public String advertisingFilterAction() {
        return node("moderation", "advertising-filter", "action").getString("warn");
    }

    public List<String> advertisingWhitelistDomains() {
        try {
            return node("moderation", "advertising-filter", "whitelist-domains").getList(String.class, List.of());
        } catch (org.spongepowered.configurate.serialize.SerializationException e) {
            return List.of();
        }
    }

    public boolean chatLogEnabled() {
        return node("moderation", "chat-log", "enabled").getBoolean(false);
    }

    public int chatLogRetentionDays() {
        return node("moderation", "chat-log", "retention-days").getInt(30);
    }

    public boolean ignoreAppliesToPublicChannels() {
        return node("ignore", "apply-to-public-channels").getBoolean(false);
    }

    /** Individually enable/disable a command name under {@code commands:} in {@code config.yml}. */
    public boolean commandEnabled(String key) {
        return node("commands", key).getBoolean(true);
    }

    public ConfigurateFile backing() {
        return file;
    }

    public void reload() throws java.io.IOException {
        file.reload();
    }
}
