package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Server-wide staff channel. Audience is every online player; {@code ChatListener} then
 * keeps only those with {@code permission.see} (by default {@code unichat.channel.admin.see}).
 */
public final class AdminChannel extends Channel {

    public AdminChannel(ChannelDefinition definition) {
        super(definition);
    }

    @Override
    public Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context) {
        return Bukkit.getOnlinePlayers();
    }
}
