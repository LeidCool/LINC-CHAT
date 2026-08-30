package com.leidcool.lincchat.moderation;

import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.storage.PlayerProfileData;

import java.util.UUID;

/**
 * Per-player ignore lists, used to filter private messages (always) and, when configured,
 * public channel messages too (TOR section 11).
 */
public final class IgnoreService {

    public boolean isIgnoring(PlayerProfileData viewer, UUID other) {
        return viewer.isIgnoring(other);
    }

    public boolean addIgnore(PlayerProfileData viewer, UUID other) {
        return viewer.addIgnored(other);
    }

    public boolean removeIgnore(PlayerProfileData viewer, UUID other) {
        return viewer.removeIgnored(other);
    }

    public boolean shouldHideFromPublicChannel(PlayerProfileData viewer, UUID sender, MainConfig config) {
        return config.ignoreAppliesToPublicChannels() && viewer.isIgnoring(sender);
    }
}
