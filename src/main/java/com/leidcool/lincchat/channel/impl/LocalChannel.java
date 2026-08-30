package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Radius + same-world limited channel, default 200 blocks (TOR section 6.3). */
public final class LocalChannel extends Channel {

    public LocalChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        return LocalRadiusResolver.nearby(sender, definition, context);
    }
}
