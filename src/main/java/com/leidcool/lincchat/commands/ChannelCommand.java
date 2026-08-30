package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.api.event.UniChatChannelSwitchEvent;
import com.leidcool.lincchat.channel.Channel;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@code /ch <channel>} and {@code /ch toggle <channel>} (TOR section 9). */
public final class ChannelCommand implements UniCommand {

    private final ChannelManager channelManager;
    private final PlayerProfileCache profiles;
    private final MessagesProvider messages;

    public ChannelCommand(ChannelManager channelManager, PlayerProfileCache profiles, MessagesProvider messages) {
        this.channelManager = channelManager;
        this.profiles = profiles;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        if (args.length == 0) {
            player.sendMessage(messages.get("channel-usage"));
            return;
        }
        if (args[0].equalsIgnoreCase("toggle") && args.length >= 2) {
            handleToggle(player, args[1]);
            return;
        }
        handleSwitch(player, args[0]);
    }

    private void handleSwitch(Player player, String id) {
        Optional<Channel> channelOpt = channelManager.get(id);
        if (channelOpt.isEmpty()) {
            player.sendMessage(messages.get("channel-unknown", Placeholder.unparsed("channel", id)));
            return;
        }
        Channel channel = channelOpt.get();
        if (!channel.isEnabled()) {
            player.sendMessage(messages.get("channel-disabled"));
            return;
        }
        if (!channelManager.canUse(player, channel, true)) {
            player.sendMessage(messages.get("channel-no-permission-speak", Placeholder.unparsed("channel", channel.displayName())));
            return;
        }
        profiles.getOrCreate(player.getUniqueId()).activeChannel(channel.id());
        Bukkit.getPluginManager().callEvent(new UniChatChannelSwitchEvent(player, channel));
        player.sendMessage(messages.get("channel-switched", Placeholder.unparsed("channel", channel.displayName())));
    }

    private void handleToggle(Player player, String id) {
        Optional<Channel> channelOpt = channelManager.get(id);
        if (channelOpt.isEmpty()) {
            player.sendMessage(messages.get("channel-unknown", Placeholder.unparsed("channel", id)));
            return;
        }
        Channel channel = channelOpt.get();
        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
        boolean nowListening = !profile.isListening(channel.id());
        profile.setListening(channel.id(), nowListening);
        player.sendMessage(messages.get(nowListening ? "channel-toggled-on" : "channel-toggled-off",
                Placeholder.unparsed("channel", channel.displayName())));
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.add("toggle");
            for (Channel channel : channelManager.usableBy(player)) {
                options.add(channel.id());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            for (Channel channel : channelManager.usableBy(player)) {
                options.add(channel.id());
            }
        }
        return options;
    }

    @Override
    public String permission() {
        return "unichat.channel.use";
    }
}
