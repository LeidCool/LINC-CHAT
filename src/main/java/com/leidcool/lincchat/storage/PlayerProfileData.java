package com.leidcool.lincchat.storage;

import com.leidcool.lincchat.api.PlayerChatProfile;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable, thread-safe (best-effort) runtime implementation of {@link PlayerChatProfile}.
 * Instances are cached in {@code ChannelManager}/service classes while a player is online and
 * persisted asynchronously through {@link PlayerDataStore}.
 */
public final class PlayerProfileData implements PlayerChatProfile {

    public static final long PERMANENT = Long.MAX_VALUE;

    private final UUID uniqueId;
    private volatile String activeChannel;
    private final Set<String> disabledChannels = ConcurrentHashMap.newKeySet();
    private volatile String nameColor;
    private volatile String prefixColor;
    private volatile String messageColor;
    private volatile long muteExpiryMillis;
    private volatile String muteReason;
    private final Map<String, MuteEntry> channelMutes = new ConcurrentHashMap<>();
    private final Set<UUID> ignoredPlayers = ConcurrentHashMap.newKeySet();
    private volatile UUID lastMessaged;
    private volatile boolean socialSpyEnabled;
    private volatile boolean mentionsEnabled = true;
    private volatile boolean dirty;

    public PlayerProfileData(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public record MuteEntry(long expiryMillis, String reason) {
        public boolean active() {
            return expiryMillis == PERMANENT || expiryMillis > System.currentTimeMillis();
        }
    }

    @Override
    public UUID uniqueId() {
        return uniqueId;
    }

    @Override
    public String activeChannel() {
        return activeChannel;
    }

    public void activeChannel(String channel) {
        this.activeChannel = channel;
        dirty = true;
    }

    @Override
    public boolean isListening(String channelId) {
        return !disabledChannels.contains(channelId);
    }

    public void setListening(String channelId, boolean listening) {
        if (listening) {
            disabledChannels.remove(channelId);
        } else {
            disabledChannels.add(channelId);
        }
        dirty = true;
    }

    @Override
    public Set<String> disabledChannels() {
        return Set.copyOf(disabledChannels);
    }

    @Override
    public Optional<String> nameColor() {
        return Optional.ofNullable(nameColor);
    }

    public void nameColor(String value) {
        this.nameColor = value;
        dirty = true;
    }

    @Override
    public Optional<String> prefixColor() {
        return Optional.ofNullable(prefixColor);
    }

    public void prefixColor(String value) {
        this.prefixColor = value;
        dirty = true;
    }

    @Override
    public Optional<String> messageColor() {
        return Optional.ofNullable(messageColor);
    }

    public void messageColor(String value) {
        this.messageColor = value;
        dirty = true;
    }

    @Override
    public boolean isMuted() {
        return muteExpiryMillis == PERMANENT || muteExpiryMillis > System.currentTimeMillis();
    }

    @Override
    public boolean isMuted(String channelId) {
        MuteEntry entry = channelMutes.get(channelId);
        return entry != null && entry.active();
    }

    @Override
    public Optional<String> muteReason() {
        return Optional.ofNullable(muteReason);
    }

    @Override
    public Optional<String> muteReason(String channelId) {
        MuteEntry entry = channelMutes.get(channelId);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.reason());
    }

    @Override
    public long muteExpiryMillis() {
        return muteExpiryMillis;
    }

    public long muteExpiryMillis(String channelId) {
        MuteEntry entry = channelMutes.get(channelId);
        return entry == null ? 0L : entry.expiryMillis();
    }

    public void mute(long expiryMillis, String reason) {
        this.muteExpiryMillis = expiryMillis;
        this.muteReason = reason;
        dirty = true;
    }

    public void unmute() {
        this.muteExpiryMillis = 0L;
        this.muteReason = null;
        dirty = true;
    }

    public void muteChannel(String channelId, long expiryMillis, String reason) {
        channelMutes.put(channelId, new MuteEntry(expiryMillis, reason));
        dirty = true;
    }

    public void unmuteChannel(String channelId) {
        channelMutes.remove(channelId);
        dirty = true;
    }

    public Map<String, MuteEntry> channelMutes() {
        return Map.copyOf(channelMutes);
    }

    @Override
    public Set<UUID> ignoredPlayers() {
        return Set.copyOf(ignoredPlayers);
    }

    @Override
    public boolean isIgnoring(UUID other) {
        return ignoredPlayers.contains(other);
    }

    public boolean addIgnored(UUID other) {
        dirty = true;
        return ignoredPlayers.add(other);
    }

    public boolean removeIgnored(UUID other) {
        dirty = true;
        return ignoredPlayers.remove(other);
    }

    @Override
    public Optional<UUID> lastMessaged() {
        return Optional.ofNullable(lastMessaged);
    }

    public void lastMessaged(UUID uuid) {
        this.lastMessaged = uuid;
        dirty = true;
    }

    @Override
    public boolean socialSpyEnabled() {
        return socialSpyEnabled;
    }

    public void socialSpyEnabled(boolean value) {
        this.socialSpyEnabled = value;
        dirty = true;
    }

    @Override
    public boolean mentionsEnabled() {
        return mentionsEnabled;
    }

    public void mentionsEnabled(boolean value) {
        this.mentionsEnabled = value;
        dirty = true;
    }

    public boolean dirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }
}
