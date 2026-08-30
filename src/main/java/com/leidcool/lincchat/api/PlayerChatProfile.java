package com.leidcool.lincchat.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public, read-only view of a player's LINC-Chat state, exposed to third-party plugins via
 * {@link UniChatAPI}. The mutable implementation lives in
 * {@code com.leidcool.lincchat.storage.PlayerProfileData}.
 */
public interface PlayerChatProfile {

    UUID uniqueId();

    /** The channel id the player currently talks in by default. */
    String activeChannel();

    /** Whether the player currently wants to see messages from the given channel. */
    boolean isListening(String channelId);

    Set<String> disabledChannels();

    Optional<String> nameColor();

    Optional<String> prefixColor();

    Optional<String> messageColor();

    boolean isMuted();

    boolean isMuted(String channelId);

    Optional<String> muteReason();

    Optional<String> muteReason(String channelId);

    /** Epoch millis when the global mute expires, {@code Long.MAX_VALUE} for a permanent mute, 0 if not muted. */
    long muteExpiryMillis();

    Set<UUID> ignoredPlayers();

    boolean isIgnoring(UUID other);

    Optional<UUID> lastMessaged();

    boolean socialSpyEnabled();

    boolean mentionsEnabled();
}
