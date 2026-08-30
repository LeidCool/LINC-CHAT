package com.leidcool.lincchat.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read-only facade for other plugins (TOR section 17 -- API/hooks for third-party
 * plugins). Obtain the active instance via {@link #get()} once LINC-Chat has finished
 * enabling (i.e. not from another plugin's {@code onLoad()}); it becomes available at the
 * start of {@code onEnable} since LINC-Chat loads before soft-dependents by declaring itself
 * as their {@code softdepend}, and is torn down in {@code onDisable}.
 */
public interface UniChatAPI {

    Collection<ChatChannel> getChannels();

    Optional<ChatChannel> getChannel(String id);

    /** Only returns a profile for a currently online player. */
    Optional<PlayerChatProfile> getProfile(UUID playerId);

    static UniChatAPI get() {
        UniChatAPI instance = UniChatAPIHolder.INSTANCE;
        if (instance == null) {
            throw new IllegalStateException("LINC-Chat API is not initialised yet");
        }
        return instance;
    }

    /** For internal use by {@code LincChatPlugin} only. */
    static void register(UniChatAPI instance) {
        UniChatAPIHolder.INSTANCE = instance;
    }

    /** For internal use by {@code LincChatPlugin} only. */
    static void unregister() {
        UniChatAPIHolder.INSTANCE = null;
    }
}
