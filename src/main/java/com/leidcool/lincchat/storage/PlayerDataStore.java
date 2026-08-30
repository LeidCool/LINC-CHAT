package com.leidcool.lincchat.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence abstraction for per-player chat state (TOR section 13). All I/O must happen
 * off the main thread -- implementations are expected to hop onto an async task themselves
 * via {@code SchedulerProvider} rather than relying on the caller to do so.
 */
public interface PlayerDataStore {

    CompletableFuture<PlayerProfileData> load(UUID uniqueId);

    CompletableFuture<Void> save(PlayerProfileData data);

    /** Releases any resources (connection pools, file handles) held by this store. */
    void close();
}
