package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.api.event.UniChatPrivateMessageEvent;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.IgnoreService;
import com.leidcool.lincchat.moderation.SpyService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Shared delivery logic for {@code /msg}/{@code /tell}/{@code /w} and {@code /r}
 * (TOR section 6.5 / 9), including the ignore check, {@code /socialspy} audience and
 * last-messaged bookkeeping used by {@code /r}.
 */
public final class PrivateMessageService {

    private final PlayerProfileCache profiles;
    private final IgnoreService ignoreService;
    private final SpyService spyService;
    private final ChannelManager channelManager;
    private final MessagesProvider messages;

    public PrivateMessageService(PlayerProfileCache profiles, IgnoreService ignoreService, SpyService spyService,
                                  ChannelManager channelManager, MessagesProvider messages) {
        this.profiles = profiles;
        this.ignoreService = ignoreService;
        this.spyService = spyService;
        this.channelManager = channelManager;
        this.messages = messages;
    }

    public boolean send(Player sender, Player target, String message) {
        if (sender.equals(target)) {
            sender.sendMessage(messages.get("pm-cannot-message-self"));
            return false;
        }
        if (channelManager.get("private").map(channel -> !channel.isEnabled()).orElse(false)) {
            sender.sendMessage(messages.get("channel-disabled"));
            return false;
        }

        PlayerProfileData targetProfile = profiles.getOrCreate(target.getUniqueId());
        if (ignoreService.isIgnoring(targetProfile, sender.getUniqueId())) {
            sender.sendMessage(messages.get("pm-blocked-ignored"));
            return false;
        }

        UniChatPrivateMessageEvent event = new UniChatPrivateMessageEvent(sender, target, message);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        String finalMessage = event.getMessage();

        PlayerProfileData senderProfile = profiles.getOrCreate(sender.getUniqueId());
        senderProfile.lastMessaged(target.getUniqueId());
        targetProfile.lastMessaged(sender.getUniqueId());

        sender.sendMessage(messages.get("pm-format-outgoing",
                Placeholder.unparsed("player", target.getName()), Placeholder.unparsed("message", finalMessage)));
        target.sendMessage(messages.get("pm-format-incoming",
                Placeholder.unparsed("player", sender.getName()), Placeholder.unparsed("message", finalMessage)));

        Component spyLine = Component.text("[Spy] " + sender.getName() + " -> " + target.getName() + ": " + finalMessage)
                .color(NamedTextColor.DARK_GRAY);
        for (Player spy : spyService.spyAudience(sender, target)) {
            spy.sendMessage(spyLine);
        }
        return true;
    }
}
