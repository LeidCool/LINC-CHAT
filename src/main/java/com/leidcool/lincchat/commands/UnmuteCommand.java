package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.MuteService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** {@code /unmute <player>} (TOR section 9/11). */
public final class UnmuteCommand implements UniCommand {

    private final PlayerProfileCache profiles;
    private final MuteService muteService;
    private final MessagesProvider messages;

    public UnmuteCommand(PlayerProfileCache profiles, MuteService muteService, MessagesProvider messages) {
        this.profiles = profiles;
        this.muteService = muteService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(messages.get("mute-usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(messages.get("player-not-found", Placeholder.unparsed("player", args[0])));
            return;
        }
        PlayerProfileData profile = profiles.getOrCreate(target.getUniqueId());
        if (!profile.isMuted()) {
            sender.sendMessage(messages.get("not-muted", Placeholder.unparsed("player", target.getName())));
            return;
        }
        muteService.unmute(profile);
        sender.sendMessage(messages.get("unmute-success", Placeholder.unparsed("player", target.getName())));
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "unichat.mute";
    }
}
