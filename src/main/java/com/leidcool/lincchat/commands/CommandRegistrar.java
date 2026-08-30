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
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;
import java.util.function.Supplier;

/**
 * Registers every LINC-Chat command (TOR section 9) through Paper's Brigadier
 * {@code Commands} API via {@code LifecycleEvents.COMMANDS} -- no {@code commands:}
 * section is needed in {@code plugin.yml} on Paper 1.21.
 */
public final class CommandRegistrar {

    private final ChannelManager channelManager;
    private final PlayerProfileCache profiles;
    private final IgnoreService ignoreService;
    private final SpyService spyService;
    private final MuteService muteService;
    private final ColorPermissionPolicy colorPermissionPolicy;
    private final MainConfig config;
    private final MessagesProvider messages;
    private final FormatEngine formatEngine;
    private final ChatPauseState chatPauseState;
    private final Runnable reloadAction;
    private final Supplier<List<String>> debugInfoSupplier;

    public CommandRegistrar(ChannelManager channelManager, PlayerProfileCache profiles, IgnoreService ignoreService,
                             SpyService spyService, MuteService muteService, ColorPermissionPolicy colorPermissionPolicy,
                             MainConfig config, MessagesProvider messages, FormatEngine formatEngine,
                             ChatPauseState chatPauseState, Runnable reloadAction, Supplier<List<String>> debugInfoSupplier) {
        this.channelManager = channelManager;
        this.profiles = profiles;
        this.ignoreService = ignoreService;
        this.spyService = spyService;
        this.muteService = muteService;
        this.colorPermissionPolicy = colorPermissionPolicy;
        this.config = config;
        this.messages = messages;
        this.formatEngine = formatEngine;
        this.chatPauseState = chatPauseState;
        this.reloadAction = reloadAction;
        this.debugInfoSupplier = debugInfoSupplier;
    }

    public void registerAll(Commands registrar) {
        for (CommandBindings.Binding binding : CommandBindings.collect(channelManager, profiles, ignoreService, spyService,
                muteService, colorPermissionPolicy, config, messages, formatEngine, chatPauseState,
                reloadAction, debugInfoSupplier)) {
            registrar.register(binding.name(), binding.description(), binding.aliases(),
                    new PaperBrigadierAdapter(binding.command()));
        }
    }
}
