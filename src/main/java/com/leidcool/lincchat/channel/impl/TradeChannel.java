package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/** Server-wide (by default) trading channel with price highlighting and optional cost (TOR section 6.4). */
public final class TradeChannel extends Channel {

    public TradeChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        if (definition.range() <= 0) {
            return Bukkit.getOnlinePlayers();
        }
        return LocalRadiusResolver.nearby(sender, definition, context);
    }

    public String priceHighlightColor() {
        String color = definition.priceHighlightColor();
        return color != null ? color : "<green>";
    }

    public double messageCost() {
        return definition.messageCost();
    }
}
