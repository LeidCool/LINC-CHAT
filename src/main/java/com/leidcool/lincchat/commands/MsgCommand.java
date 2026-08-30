package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** {@code /msg}/{@code /tell}/{@code /w} (TOR section 9). */
public final class MsgCommand implements UniCommand {

    private final PrivateMessageService pmService;
    private final MessagesProvider messages;

    public MsgCommand(PrivateMessageService pmService, MessagesProvider messages) {
        this.pmService = pmService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(messages.get("msg-usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(messages.get("player-not-found", Placeholder.unparsed("player", args[0])));
            return;
        }
        String message = String.join(" ", Arrays.asList(args).subList(1, args.length));
        pmService.send(player, target, message);
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
        return "unichat.pm.send";
    }
}
