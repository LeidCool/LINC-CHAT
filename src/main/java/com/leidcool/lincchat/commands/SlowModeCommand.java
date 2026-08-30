package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.ChatPauseState;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;

/** {@code /slowmode <seconds|off>} (TOR section 9/11 -- Definition of Done item). */
public final class SlowModeCommand implements UniCommand {

    private final ChatPauseState state;
    private final MessagesProvider messages;

    public SlowModeCommand(ChatPauseState state, MessagesProvider messages) {
        this.state = state;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(messages.get("slowmode-usage"));
            return;
        }
        if (args[0].equalsIgnoreCase("off")) {
            state.setSlowModeSeconds(0);
            sender.sendMessage(messages.get("slowmode-off"));
            return;
        }
        try {
            int seconds = Integer.parseInt(args[0]);
            state.setSlowModeSeconds(seconds);
            sender.sendMessage(messages.get("slowmode-set", Placeholder.unparsed("seconds", String.valueOf(seconds))));
        } catch (NumberFormatException ex) {
            sender.sendMessage(messages.get("slowmode-usage"));
        }
    }

    @Override
    public Collection<String> suggest(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("off", "5", "10", "30");
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "unichat.pause";
    }
}
