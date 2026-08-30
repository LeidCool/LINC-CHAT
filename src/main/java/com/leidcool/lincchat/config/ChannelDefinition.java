package com.leidcool.lincchat.config;

import com.leidcool.lincchat.channel.ChannelType;

import org.jetbrains.annotations.Nullable;

/**
 * Raw, immutable snapshot of a single channel block from {@code channels.yml}. Consumed by
 * {@code ChannelManager} to build the actual {@code Channel} implementations. Kept separate
 * from the runtime {@code Channel} model so a {@code /unichat reload} can rebuild channels
 * from a fresh snapshot without touching per-player runtime state.
 */
public record ChannelDefinition(
        String id,
        boolean enabled,
        String displayName,
        String tag,
        @Nullable String tagOutgoing,
        @Nullable String tagIncoming,
        String format,
        boolean defaultChannel,
        int range,
        boolean verticalCheck,
        int cooldownSeconds,
        @Nullable String shortcut,
        boolean itemLinkEnabled,
        @Nullable String speakPermission,
        @Nullable String seePermission,
        @Nullable String priceHighlightColor,
        double messageCost,
        boolean notifyIfEmpty,
        ChannelType type
) {
}
