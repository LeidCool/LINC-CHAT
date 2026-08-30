package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Any admin-defined channel in {@code channels.yml} that is not one of the built-in
 * ids (TOR section 6.6, e.g. {@code staff}, {@code rp}, {@code event}). Behaves like Global
 * when {@code range: 0}, or like Local when a positive range is configured.
 */
public final class CustomChannel extends Channel {

    public CustomChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        if (definition.range() <= 0) {
            return Bukkit.getOnlinePlayers();
        }
        return LocalRadiusResolver.nearby(sender, definition, context);
    }
}
