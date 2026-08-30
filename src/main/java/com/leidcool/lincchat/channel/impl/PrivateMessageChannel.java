package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/**
 * Not a broadcast channel: {@code /msg}/{@code /tell}/{@code /w}/{@code /r} address a specific
 * player directly (TOR section 6.5). This class only carries the channel's enabled flag and
 * tag configuration so it can be toggled off/reconfigured like any other channel.
 */
public final class PrivateMessageChannel extends Channel {

    public PrivateMessageChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        return List.of();
    }

    public String tagOutgoing() {
        return definition.tagOutgoing() != null ? definition.tagOutgoing() : definition.tag();
    }

    public String tagIncoming() {
        return definition.tagIncoming() != null ? definition.tagIncoming() : definition.tag();
    }
}
