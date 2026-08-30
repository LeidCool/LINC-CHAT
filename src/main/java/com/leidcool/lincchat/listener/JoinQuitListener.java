package com.leidcool.lincchat.listener;

import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.moderation.AntiSpamService;
import com.leidcool.lincchat.storage.PlayerDataStore;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Loads a player's {@link PlayerProfileData} asynchronously on join and caches it, then
 * persists and evicts it on quit (TOR section 13). Also clears any per-player transient
 * moderation state to avoid an unbounded memory leak across many join/quit cycles
 * (TOR section 16).
 */
public final class JoinQuitListener implements Listener {

    private final PlayerDataStore store;
    private final PlayerProfileCache cache;
    private final ChannelManager channelManager;
    private final AntiSpamService antiSpam;

    public JoinQuitListener(PlayerDataStore store, PlayerProfileCache cache, ChannelManager channelManager,
                             AntiSpamService antiSpam) {
        this.store = store;
        this.cache = cache;
        this.channelManager = channelManager;
        this.antiSpam = antiSpam;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        store.load(uuid).thenAccept(data -> {
            if (data.activeChannel() == null) {
                channelManager.defaultChannel().ifPresent(channel -> data.activeChannel(channel.id()));
            }
            cache.put(uuid, data);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cache.get(uuid).ifPresent(store::save);
        cache.remove(uuid);
        antiSpam.clear(uuid);
    }
}
