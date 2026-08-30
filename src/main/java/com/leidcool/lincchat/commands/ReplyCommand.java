package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** {@code /r <message>} -- replies to the last player who messaged/was messaged (TOR section 9). */
public final class ReplyCommand implements UniCommand {

    private final PrivateMessageService pmService;
    private final PlayerProfileCache profiles;
    private final MessagesProvider messages;

    public ReplyCommand(PrivateMessageService pmService, PlayerProfileCache profiles, MessagesProvider messages) {
        this.pmService = pmService;
        this.profiles = profiles;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(messages.get("reply-usage"));
            return;
        }
        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
        UUID lastId = profile.lastMessaged().orElse(null);
        if (lastId == null) {
            player.sendMessage(messages.get("no-reply-target"));
            return;
        }
        Player target = Bukkit.getPlayer(lastId);
        if (target == null) {
            player.sendMessage(messages.get("player-must-be-online"));
            return;
        }
        String message = String.join(" ", args);
        pmService.send(player, target, message);
    }

    @Override
    public String permission() {
        return "unichat.pm.send";
    }
}
