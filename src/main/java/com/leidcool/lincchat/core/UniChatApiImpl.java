package com.leidcool.lincchat.core;

import com.leidcool.lincchat.api.ChatChannel;
import com.leidcool.lincchat.api.PlayerChatProfile;
import com.leidcool.lincchat.api.UniChatAPI;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.storage.PlayerProfileCache;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** Default {@link UniChatAPI} implementation, backed directly by the live service objects. */
public final class UniChatApiImpl implements UniChatAPI {

    private final ChannelManager channelManager;
    private final PlayerProfileCache profiles;

    public UniChatApiImpl(ChannelManager channelManager, PlayerProfileCache profiles) {
        this.channelManager = channelManager;
        this.profiles = profiles;
    }

    @Override
    public Collection<ChatChannel> getChannels() {
        return channelManager.all().stream().map(channel -> (ChatChannel) channel).toList();
    }

    @Override
    public Optional<ChatChannel> getChannel(String id) {
        return channelManager.get(id).map(channel -> (ChatChannel) channel);
    }

    @Override
    public Optional<PlayerChatProfile> getProfile(UUID playerId) {
        return profiles.get(playerId).map(profile -> (PlayerChatProfile) profile);
    }
}
