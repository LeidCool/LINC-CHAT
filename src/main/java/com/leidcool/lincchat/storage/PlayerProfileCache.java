package com.leidcool.lincchat.storage;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of online players' {@link PlayerProfileData}. Populated by
 * {@code JoinQuitListener} on join (after an async load from {@link PlayerDataStore}) and
 * cleared on quit (after an async save). Every other service (channels, moderation, colours,
 * commands, the PlaceholderAPI expansion) reads/writes through this cache instead of hitting
 * the store directly, keeping disk I/O off the hot path (TOR section 16).
 */
public final class PlayerProfileCache {

    private final Map<UUID, PlayerProfileData> online = new ConcurrentHashMap<>();

    public PlayerProfileData getOrCreate(UUID uuid) {
        return online.computeIfAbsent(uuid, PlayerProfileData::new);
    }

    public Optional<PlayerProfileData> get(UUID uuid) {
        return Optional.ofNullable(online.get(uuid));
    }

    public void put(UUID uuid, PlayerProfileData data) {
        online.put(uuid, data);
    }

    public void remove(UUID uuid) {
        online.remove(uuid);
    }

    /** Snapshot of every cached profile, used for the shutdown save sweep in {@code onDisable}. */
    public Collection<PlayerProfileData> all() {
        return online.values();
    }
}
