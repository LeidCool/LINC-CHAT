package com.leidcool.lincchat.integration;

import com.leidcool.lincchat.channel.Channel;
import org.bukkit.entity.Player;

/**
 * Extension point for region-based channel restrictions (TOR section 5.4: WorldGuard flags
 * to disable a channel inside a region, e.g. a minigame arena).
 * <p>
 * Phase 4 / not implemented in this pass: no WorldGuard-backed implementation is registered
 * yet, only this interface and {@link #ALLOW_ALL}. A future {@code WorldGuardHook} would
 * implement this against the WorldGuard region flag API and be registered in
 * {@code LincChatPlugin} only when WorldGuard is present.
 */
public interface ChannelAccessGuard {

    ChannelAccessGuard ALLOW_ALL = (player, channel) -> true;

    boolean canUseChannel(Player player, Channel channel);
}
