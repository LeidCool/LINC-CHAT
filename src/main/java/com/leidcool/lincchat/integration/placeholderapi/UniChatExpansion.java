package com.leidcool.lincchat.integration.placeholderapi;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.channel.impl.LocalRadiusResolver;
import com.leidcool.lincchat.color.ColorProfile;
import com.leidcool.lincchat.color.ColorResolver;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the {@code %unichat_*%} placeholders described in TOR section 5.3.
 */
public final class UniChatExpansion extends PlaceholderExpansion {

    private final ChannelManager channelManager;
    private final ColorResolver colorResolver;
    private final PlayerProfileCache profiles;
    private final String version;

    public UniChatExpansion(ChannelManager channelManager, ColorResolver colorResolver,
                             PlayerProfileCache profiles, String version) {
        this.channelManager = channelManager;
        this.colorResolver = colorResolver;
        this.profiles = profiles;
        this.version = version;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "unichat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "leidcool";
    }

    @Override
    public @NotNull String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());

        if (params.equals("channel")) {
            return profile.activeChannel() != null ? profile.activeChannel() : "";
        }
        if (params.startsWith("channel_tag_")) {
            String id = params.substring("channel_tag_".length());
            return channelManager.get(id).map(Channel::tag).orElse("");
        }
        if (params.equals("prefix_color") || params.equals("name_color") || params.equals("message_color")) {
            ColorProfile colors = colorResolver.resolve(player, profile);
            return switch (params) {
                case "prefix_color" -> colors.prefixColor();
                case "name_color" -> colors.nameColor();
                default -> colors.messageColor();
            };
        }
        if (params.equals("local_radius")) {
            return channelManager.get("local")
                    .map(channel -> {
                        int radius = LocalRadiusResolver.effectiveRadius(player, channel.definition(), channelManager.context());
                        return radius <= 0 ? "unlimited" : String.valueOf(radius);
                    })
                    .orElse("0");
        }
        if (params.equals("muted")) {
            return String.valueOf(profile.isMuted());
        }
        if (params.equals("ignoring_count")) {
            return String.valueOf(profile.ignoredPlayers().size());
        }
        return null;
    }
}
