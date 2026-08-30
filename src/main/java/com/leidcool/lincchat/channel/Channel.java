package com.leidcool.lincchat.channel;

import com.leidcool.lincchat.api.ChatChannel;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Internal domain model for a chat channel (TOR section 6.1 / 4). Concrete behaviour
 * (who receives a message) is provided by the {@code channel.impl} subclasses.
 */
public abstract class Channel implements ChatChannel {

    protected final ChannelDefinition definition;

    protected Channel(ChannelDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String id() {
        return definition.id();
    }

    @Override
    public String displayName() {
        return definition.displayName();
    }

    @Override
    public boolean isEnabled() {
        return definition.enabled();
    }

    @Override
    public ChannelType type() {
        return definition.type();
    }

    @Override
    public String tag() {
        return definition.tag();
    }

    public ChannelDefinition definition() {
        return definition;
    }

    public boolean canSpeak(Player player) {
        String node = definition.speakPermission();
        return node == null || node.isBlank() || player.hasPermission(node);
    }

    public boolean canSee(Player player) {
        String node = definition.seePermission();
        return node == null || node.isBlank() || player.hasPermission(node);
    }

    /**
     * Returns the raw candidate audience for this channel, before ignore/mute/personal
     * listening-toggle filters are applied by {@code ChatListener}. Implementations must
     * stay cheap even with a high player count (TOR section 16) -- e.g. {@link
     * com.leidcool.lincchat.channel.impl.LocalRadiusResolver} uses
     * {@code World#getNearbyEntities} rather than an all-players distance scan.
     */
    public abstract Collection<? extends Player> candidateRecipients(Player sender, ChannelContext context);
}
