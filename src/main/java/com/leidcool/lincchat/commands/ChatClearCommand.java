package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /chatclear} -- pushes blank lines to every online player (TOR section 9/11). */
public final class ChatClearCommand implements UniCommand {

    private static final int BLANK_LINES = 100;

    private final MessagesProvider messages;

    public ChatClearCommand(MessagesProvider messages) {
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < BLANK_LINES; i++) {
                online.sendMessage(Component.empty());
            }
        }
        sender.sendMessage(messages.get("chatclear-success"));
    }

    @Override
    public String permission() {
        return "unichat.clear";
    }
}
