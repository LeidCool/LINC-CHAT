package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.moderation.ChatPauseState;
import org.bukkit.command.CommandSender;

/** {@code /chatpause} toggle (TOR section 9/11 -- Definition of Done item). */
public final class ChatPauseCommand implements UniCommand {

    private final ChatPauseState state;
    private final MessagesProvider messages;

    public ChatPauseCommand(ChatPauseState state, MessagesProvider messages) {
        this.state = state;
        this.messages = messages;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean newState = !state.isPaused();
        state.setPaused(newState);
        sender.sendMessage(messages.get(newState ? "chat-paused" : "chat-unpaused"));
    }

    @Override
    public String permission() {
        return "unichat.pause";
    }
}
