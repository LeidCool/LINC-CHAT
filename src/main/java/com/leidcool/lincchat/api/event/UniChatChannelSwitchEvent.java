package com.leidcool.lincchat.api.event;

import com.leidcool.lincchat.api.ChatChannel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player switches their active LINC-Chat channel via {@code /ch} (TOR section
 * 17 -- public API extension point for third-party plugins).
 */
public final class UniChatChannelSwitchEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ChatChannel channel;

    public UniChatChannelSwitchEvent(Player player, ChatChannel channel) {
        this.player = player;
        this.channel = channel;
    }

    public Player getPlayer() {
        return player;
    }

    public ChatChannel getChannel() {
        return channel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
