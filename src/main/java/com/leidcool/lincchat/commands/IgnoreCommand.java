package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.IgnoreService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** {@code /ignore <player>} and {@code /unignore <player>} (TOR section 9/11). */
public final class IgnoreCommand implements UniCommand {

    private final boolean addMode;
    private final PlayerProfileCache profiles;
    private final IgnoreService ignoreService;
    private final MessagesProvider messages;

    public IgnoreCommand(boolean addMode, PlayerProfileCache profiles, IgnoreService ignoreService, MessagesProvider messages) {
        this.addMode = addMode;
        this.profiles = profiles;
        this.ignoreService = ignoreService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(messages.get("ignore-usage"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(messages.get("player-not-found", Placeholder.unparsed("player", args[0])));
            return;
        }
        if (target.equals(player)) {
            player.sendMessage(messages.get("cannot-ignore-self"));
            return;
        }

        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
        if (addMode) {
            boolean added = ignoreService.addIgnore(profile, target.getUniqueId());
            player.sendMessage(messages.get(added ? "ignored-player" : "already-ignored", Placeholder.unparsed("player", target.getName())));
        } else {
            boolean removed = ignoreService.removeIgnore(profile, target.getUniqueId());
            player.sendMessage(messages.get(removed ? "unignored-player" : "not-ignored", Placeholder.unparsed("player", target.getName())));
        }
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
        return "unichat.ignore";
    }
}
