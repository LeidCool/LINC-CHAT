package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.color.ColorPermissionPolicy;
import com.leidcool.lincchat.color.MiniMessageSanitizer;
import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import com.leidcool.lincchat.util.LegacyColorConverter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** {@code /chatcolor <name|prefix|message> <colour>} and {@code /chatcolor reset [layer]} (TOR section 8/9). */
public final class ChatColorCommand implements UniCommand {

    private final PlayerProfileCache profiles;
    private final ColorPermissionPolicy policy;
    private final MainConfig config;
    private final MessagesProvider messages;

    public ChatColorCommand(PlayerProfileCache profiles, ColorPermissionPolicy policy, MainConfig config, MessagesProvider messages) {
        this.profiles = profiles;
        this.policy = policy;
        this.config = config;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("console-cannot-use"));
            return;
        }
        if (!policy.canUseSelfColor(player)) {
            player.sendMessage(messages.get("color-no-permission"));
            return;
        }
        if (args.length < 1) {
            player.sendMessage(messages.get("color-usage"));
            return;
        }

        PlayerProfileData profile = profiles.getOrCreate(player.getUniqueId());

        if (args[0].equalsIgnoreCase("reset")) {
            String layer = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
            resetLayer(profile, layer);
            player.sendMessage(messages.get("color-reset", Placeholder.unparsed("layer", layer)));
            return;
        }

        String layer = args[0].toLowerCase(Locale.ROOT);
        if (!layer.equals("name") && !layer.equals("prefix") && !layer.equals("message")) {
            player.sendMessage(messages.get("color-usage"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(messages.get("color-usage"));
            return;
        }

        String rawInput = String.join(" ", Arrays.asList(args).subList(1, args.length));
        if (config.allowLegacyColorCodesInput()) {
            rawInput = LegacyColorConverter.toMiniMessage(rawInput);
        }

        ColorPermissionPolicy.Tier tier = policy.tierFor(player);
        String sanitized;
        try {
            sanitized = MiniMessageSanitizer.sanitize(rawInput, tier);
        } catch (MiniMessageSanitizer.RejectedException ex) {
            player.sendMessage(messages.get("color-invalid", Placeholder.unparsed("value", rawInput)));
            return;
        }

        switch (layer) {
            case "name" -> profile.nameColor(sanitized);
            case "prefix" -> profile.prefixColor(sanitized);
            default -> profile.messageColor(sanitized);
        }
        player.sendMessage(messages.get("color-set", Placeholder.unparsed("layer", layer), Placeholder.parsed("value", sanitized)));
    }

    private void resetLayer(PlayerProfileData profile, String layer) {
        switch (layer) {
            case "name" -> profile.nameColor(null);
            case "prefix" -> profile.prefixColor(null);
            case "message" -> profile.messageColor(null);
            default -> {
                profile.nameColor(null);
                profile.prefixColor(null);
                profile.messageColor(null);
            }
        }
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return List.of("name", "prefix", "message", "reset");
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "unichat.color.self";
    }
}
