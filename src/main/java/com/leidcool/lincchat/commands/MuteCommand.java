package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.MuteService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import com.leidcool.lincchat.util.DurationFormatter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@code /mute <player> [duration] [reason]} (TOR section 9/11). */
public final class MuteCommand implements UniCommand {

    private final PlayerProfileCache profiles;
    private final MuteService muteService;
    private final MessagesProvider messages;

    public MuteCommand(PlayerProfileCache profiles, MuteService muteService, MessagesProvider messages) {
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

        Optional<MuteService.ParsedDuration> parsed = args.length >= 2 ? MuteService.parseDuration(args[1]) : Optional.empty();
        MuteService.ParsedDuration duration = parsed.orElse(MuteService.ParsedDuration.ofPermanent());
        int reasonStart = parsed.isPresent() ? 2 : 1;
        String reason = args.length > reasonStart ? String.join(" ", Arrays.asList(args).subList(reasonStart, args.length)) : "-";

        PlayerProfileData profile = profiles.getOrCreate(target.getUniqueId());
        muteService.mute(profile, duration, reason);

        String timeText = duration.permanent() ? "∞" : DurationFormatter.format(duration.millis());
        sender.sendMessage(messages.get(duration.permanent() ? "mute-success-permanent" : "mute-success",
                Placeholder.unparsed("player", target.getName()),
                Placeholder.unparsed("time", timeText),
                Placeholder.unparsed("reason", reason)));
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 2) {
            return List.of("10m", "1h", "1d", "perm");
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "unichat.mute";
    }
}
