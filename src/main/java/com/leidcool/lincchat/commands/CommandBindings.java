package com.leidcool.lincchat.commands;

import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.color.ColorPermissionPolicy;
import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.config.MessagesProvider;
import com.leidcool.lincchat.format.FormatEngine;
import com.leidcool.lincchat.moderation.ChatPauseState;
import com.leidcool.lincchat.moderation.IgnoreService;
import com.leidcool.lincchat.moderation.MuteService;
import com.leidcool.lincchat.moderation.SpyService;
import com.leidcool.lincchat.storage.PlayerProfileCache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Builds the set of commands that should be registered for the current config
 * (TOR section 9). Platform-specific registrars (Brigadier vs Bukkit) only iterate this list.
 */
public final class CommandBindings {

    public record Binding(String name, String description, List<String> aliases, UniCommand command) {
    }

    private CommandBindings() {
    }

    public static List<Binding> collect(ChannelManager channelManager, PlayerProfileCache profiles,
                                        IgnoreService ignoreService, SpyService spyService, MuteService muteService,
                                        ColorPermissionPolicy colorPermissionPolicy, MainConfig config,
                                        MessagesProvider messages, FormatEngine formatEngine,
                                        ChatPauseState chatPauseState, Runnable reloadAction,
                                        Supplier<List<String>> debugInfoSupplier) {
        List<Binding> bindings = new ArrayList<>();
        PrivateMessageService pmService = new PrivateMessageService(profiles, ignoreService, spyService, channelManager, messages);

        MsgCommand msgCommand = new MsgCommand(pmService, messages);
        if (config.commandEnabled("msg")) {
            bindings.add(new Binding("msg", "Send a private message", List.of("tell", "w", "whisper"), msgCommand));
        }
        if (config.commandEnabled("m")) {
            bindings.add(new Binding("m", "Send a private message", List.of(), msgCommand));
        }
        if (config.commandEnabled("me")) {
            bindings.add(new Binding("me", "Send a private message", List.of(), msgCommand));
        }
        if (config.commandEnabled("r")) {
            bindings.add(new Binding("r", "Reply to the last private message", List.of("reply"),
                    new ReplyCommand(pmService, profiles, messages)));
        }

        bindings.add(new Binding("ignore", "Ignore a player's messages", List.of(),
                new IgnoreCommand(true, profiles, ignoreService, messages)));
        bindings.add(new Binding("unignore", "Stop ignoring a player", List.of(),
                new IgnoreCommand(false, profiles, ignoreService, messages)));
        bindings.add(new Binding("socialspy", "Toggle seeing everyone's private messages", List.of("spy"),
                new SocialSpyCommand(profiles, spyService, messages)));
        bindings.add(new Binding("chatcolor", "Set your personal chat colours", List.of("cc"),
                new ChatColorCommand(profiles, colorPermissionPolicy, config, messages)));
        bindings.add(new Binding("mute", "Mute a player", List.of(),
                new MuteCommand(profiles, muteService, messages)));
        bindings.add(new Binding("unmute", "Unmute a player", List.of(),
                new UnmuteCommand(profiles, muteService, messages)));
        bindings.add(new Binding("chatclear", "Clear everyone's chat", List.of("clearchat"),
                new ChatClearCommand(messages)));
        bindings.add(new Binding("chatpause", "Freeze/unfreeze all chat", List.of(),
                new ChatPauseCommand(chatPauseState, messages)));
        bindings.add(new Binding("slowmode", "Set a server-wide minimum chat cooldown", List.of(),
                new SlowModeCommand(chatPauseState, messages)));
        bindings.add(new Binding("unichat", "LINC-Chat administration", List.of(),
                new UniChatCommand(reloadAction, debugInfoSupplier, profiles, messages)));
        return bindings;
    }
}
