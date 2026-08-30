package com.leidcool.lincchat.integration;

import com.leidcool.lincchat.channel.Channel;
import org.bukkit.entity.Player;

/**
 * Extension point for bridging chat to an external service (TOR section 5.4 / 12: DiscordSRV
 * or a custom webhook).
 * <p>
 * Phase 4 / not implemented in this pass: {@link #NOOP} is the only registered
 * implementation. A future {@code DiscordBridge} would implement this and be wired up only
 * when {@code integrations.discord.enabled} is set and DiscordSRV (or a webhook URL) is
 * configured.
 */
public interface ChatBridge {

    ChatBridge NOOP = (channel, sender, plainMessage) -> {
    };

    void broadcastToExternal(Channel channel, Player sender, String plainMessage);
}
