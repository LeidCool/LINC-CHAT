package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Server-wide channel, visible to every online player (TOR section 6.2). */
public final class GlobalChannel extends Channel {

    public GlobalChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        return Bukkit.getOnlinePlayers();
    }
}
