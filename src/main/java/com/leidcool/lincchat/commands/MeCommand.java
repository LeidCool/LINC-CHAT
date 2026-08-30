package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.format.FormatEngine;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import com.leidcool.lincchat.util.DurationFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /me <action>} roleplay command (TOR Definition of Done item). */
public final class MeCommand implements UniCommand {

    private final FormatEngine formatEngine;
    private final PlayerProfileCache profiles;
    private final MessagesProvider messages;

    public MeCommand(FormatEngine formatEngine, PlayerProfileCache profiles, MessagesProvider messages) {
        this.formatEngine = formatEngine;
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
            player.sendMessage(messages.get("me-usage"));
            return;
        }

        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
        if (profile.isMuted() && !player.hasPermission("unichat.bypass.mute")) {
            player.sendMessage(messages.get("muted", Placeholder.unparsed("time", DurationFormatter.formatRemaining(profile.muteExpiryMillis()))));
            return;
        }

        String action = String.join(" ", args);
        Component component = formatEngine.renderMe(player, action);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(component);
        }
    }

    @Override
    public String permission() {
        return "unichat.me";
    }
}
