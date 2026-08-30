package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** {@code /unichat reload|debug|mentions} (TOR sections 9/15 -- Definition of Done item). */
public final class UniChatCommand implements UniCommand {

    private final Runnable reloadAction;
    private final Supplier<List<String>> debugInfoSupplier;
    private final PlayerProfileCache profiles;
    private final MessagesProvider messages;

    public UniChatCommand(Runnable reloadAction, Supplier<List<String>> debugInfoSupplier,
                           PlayerProfileCache profiles, MessagesProvider messages) {
        this.reloadAction = reloadAction;
        this.debugInfoSupplier = debugInfoSupplier;
        this.profiles = profiles;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messages.get("unichat-usage"));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("unichat.admin.reload")) {
                    sender.sendMessage(messages.get("generic-no-permission"));
                    return;
                }
                reloadAction.run();
                sender.sendMessage(messages.get("reload-success"));
            }
            case "debug" -> {
                if (!sender.hasPermission("unichat.admin.debug")) {
                    sender.sendMessage(messages.get("generic-no-permission"));
                    return;
                }
                sender.sendMessage(messages.get("debug-header"));
                for (String line : debugInfoSupplier.get()) {
                    sender.sendMessage(Component.text(line));
                }
            }
            case "mentions" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(messages.get("console-cannot-use"));
                    return;
                }
                if (!player.hasPermission("unichat.mentions.toggle")) {
                    sender.sendMessage(messages.get("generic-no-permission"));
                    return;
                }
                PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());
                profile.mentionsEnabled(!profile.mentionsEnabled());
                player.sendMessage(messages.get(profile.mentionsEnabled() ? "mentions-toggle-on" : "mentions-toggle-off"));
            }
            default -> sender.sendMessage(messages.get("unichat-usage"));
        }
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return List.of("reload", "debug", "mentions");
        }
        return List.of();
    }
}
