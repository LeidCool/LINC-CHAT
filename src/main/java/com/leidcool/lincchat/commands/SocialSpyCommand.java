package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.SpyService;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /socialspy} toggle (TOR section 9/11). */
public final class SocialSpyCommand implements UniCommand {

    private final PlayerProfileCache profiles;
    private final SpyService spyService;
    private final MessagesProvider messages;

    public SocialSpyCommand(PlayerProfileCache profiles, SpyService spyService, MessagesProvider messages) {
        this.profiles = profiles;
        this.spyService = spyService;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
        spyService.toggle(profile);
        player.sendMessage(messages.get(profile.socialSpyEnabled() ? "socialspy-on" : "socialspy-off"));
    }

    @Override
    public String permission() {
        return "unichat.socialspy";
    }
}
