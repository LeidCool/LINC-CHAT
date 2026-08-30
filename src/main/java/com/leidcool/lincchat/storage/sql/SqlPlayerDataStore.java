package com.leidcool.lincchat.storage.sql;

import com.leidcool.lincchat.storage.PlayerDataStore;
import com.leidcool.lincchat.storage.PlayerProfileData;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Architectural placeholder for a future HikariCP-backed SQLite/MySQL storage implementation
 * (TOR section 13 / Phase 4). Not implemented in this pass.
 * <p>
 * {@code config.yml#storage.type} only supports {@code yaml} today; if an admin selects
 * {@code sqlite} or {@code mysql}, {@code LincChatPlugin} logs a warning and falls back to
 * {@link com.leidcool.lincchat.storage.yaml.YamlPlayerDataStore} instead of instantiating
 * this class. A real implementation would additionally need a versioned {@code schema_version}
 * table/migration runner, as called for by the TOR.
 */
public final class SqlPlayerDataStore implements PlayerDataStore {

    public SqlPlayerDataStore() {
        throw new UnsupportedOperationException(
                "SQL storage is not implemented yet in this build; use storage.type: yaml");
    }

    @Override
    public CompletableFuture<PlayerProfileData> load(UUID uniqueId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> save(PlayerProfileData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // no-op
    }
}
