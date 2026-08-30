package com.leidcool.lincchat;

import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.channel.ChannelManager;
import com.leidcool.lincchat.color.ColorPermissionPolicy;
import com.leidcool.lincchat.color.ColorResolver;
import com.leidcool.lincchat.commands.CommandRegistrar;
import com.leidcool.lincchat.config.ChannelsConfig;
import com.leidcool.lincchat.config.ConfigurateFile;
import com.leidcool.lincchat.config.MainConfig;
import com.leidcool.lincchat.config.MessagesConfig;
import com.leidcool.lincchat.core.UniChatApiImpl;
import com.leidcool.lincchat.format.FormatEngine;
import com.leidcool.lincchat.integration.ChatConflictWarner;
import com.leidcool.lincchat.integration.EconomyProvider;
import com.leidcool.lincchat.integration.NoopEconomyProvider;
import com.leidcool.lincchat.integration.NoopPermissionsProvider;
import com.leidcool.lincchat.integration.NoopPlaceholderProvider;
import com.leidcool.lincchat.integration.PermissionsProvider;
import com.leidcool.lincchat.integration.PlaceholderProvider;
import com.leidcool.lincchat.integration.SchedulerProvider;
import com.leidcool.lincchat.integration.luckperms.LuckPermsHook;
import com.leidcool.lincchat.integration.paper.PaperSchedulerProvider;
import com.leidcool.lincchat.integration.placeholderapi.PapiPlaceholderProvider;
import com.leidcool.lincchat.integration.placeholderapi.UniChatExpansion;
import com.leidcool.lincchat.integration.vault.VaultEconomyHook;
import com.leidcool.lincchat.integration.vault.VaultPermissionHook;
import com.leidcool.lincchat.api.UniChatAPI;
import com.leidcool.lincchat.listener.ChatListener;
import com.leidcool.lincchat.listener.JoinQuitListener;
import com.leidcool.lincchat.listener.PermissionChangeListener;
import com.leidcool.lincchat.moderation.AntiSpamService;
import com.leidcool.lincchat.moderation.ChatLogService;
import com.leidcool.lincchat.moderation.ChatPauseState;
import com.leidcool.lincchat.moderation.IgnoreService;
import com.leidcool.lincchat.moderation.MuteService;
import com.leidcool.lincchat.moderation.SpyService;
import com.leidcool.lincchat.moderation.SwearFilterService;
import com.leidcool.lincchat.storage.PlayerDataStore;
import com.leidcool.lincchat.storage.PlayerProfileCache;
import com.leidcool.lincchat.storage.PlayerProfileData;
import com.leidcool.lincchat.storage.yaml.YamlPlayerDataStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

/**
 * Main plugin bootstrap: loads configuration, wires every service described across the TOR
 * (integrations -&gt; storage -&gt; channels -&gt; colours/format -&gt; moderation -&gt; listeners -&gt; commands
 * -&gt; public API) and exposes {@code /unichat reload}/{@code debug} for operators.
 */
public final class LincChatPlugin extends JavaPlugin {

    private MainConfig mainConfig;
    private ChannelsConfig channelsConfig;
    private MessagesConfig messagesConfig;

    private SchedulerProvider scheduler;
    private PermissionsProvider permissionsProvider;
    private EconomyProvider economyProvider;
    private PlaceholderProvider placeholderProvider;
    private LuckPermsHook luckPermsHook;

    private PlayerDataStore playerDataStore;
    private PlayerProfileCache profileCache;

    private ChannelManager channelManager;
    private ColorResolver colorResolver;
    private ColorPermissionPolicy colorPermissionPolicy;
    private FormatEngine formatEngine;

    private AntiSpamService antiSpamService;
    private SwearFilterService swearFilterService;
    private MuteService muteService;
    private IgnoreService ignoreService;
    private SpyService spyService;
    private ChatLogService chatLogService;
    private ChatPauseState chatPauseState;

    private UniChatExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        try {
            loadConfigs();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load configuration, disabling LINC-Chat", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.scheduler = new PaperSchedulerProvider(this);
        setupIntegrations();
        warnIfUnsupportedStorage();
        ChatConflictWarner.warnIfPresent(this);

        this.playerDataStore = new YamlPlayerDataStore(this, scheduler);
        this.profileCache = new PlayerProfileCache();

        ChannelContext channelContext = new ChannelContext(permissionsProvider);
        this.channelManager = new ChannelManager(this, channelContext);
        channelManager.load(channelsConfig);

        this.colorResolver = new ColorResolver(permissionsProvider, mainConfig);
        this.colorPermissionPolicy = new ColorPermissionPolicy();
        this.formatEngine = new FormatEngine(mainConfig, permissionsProvider, economyProvider, placeholderProvider,
                colorPermissionPolicy);

        this.antiSpamService = new AntiSpamService(mainConfig);
        this.swearFilterService = new SwearFilterService(mainConfig);
        this.muteService = new MuteService();
        this.ignoreService = new IgnoreService();
        this.spyService = new SpyService(profileCache);
        this.chatLogService = new ChatLogService(this, scheduler, mainConfig);
        this.chatPauseState = new ChatPauseState();

        registerListeners();
        registerCommands();
        registerPlaceholderExpansionIfPresent();

        UniChatAPI.register(new UniChatApiImpl(channelManager, profileCache));

        chatLogService.pruneOldLogs();

        getLogger().info("LINC-Chat enabled. storage=" + mainConfig.storageType()
                + " permissions=" + permissionsProvider.name()
                + " economy=" + economyProvider.isEnabled()
                + " placeholders=" + placeholderProvider.isEnabled()
                + " channels=" + channelManager.all().size());
    }

    @Override
    public void onDisable() {
        UniChatAPI.unregister();
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        // Best-effort: PlayerProfileData for players still online at shutdown is saved here.
        // Individual joins/quits are already persisted synchronously by JoinQuitListener; this
        // sweep only covers players who never quit (e.g. a full server stop/restart), and relies
        // on the storage backend's async save completing before the JVM exits.
        if (playerDataStore != null && profileCache != null) {
            for (PlayerProfileData data : profileCache.all()) {
                playerDataStore.save(data);
            }
        }
        if (playerDataStore != null) {
            playerDataStore.close();
        }
        getLogger().info("LINC-Chat disabled.");
    }

    private void loadConfigs() throws IOException {
        ConfigurateFile mainFile = ConfigurateFile.loadOrCreate(this, "config.yml");
        this.mainConfig = new MainConfig(mainFile);

        ConfigurateFile channelsFile = ConfigurateFile.loadOrCreate(this, "channels.yml");
        this.channelsConfig = new ChannelsConfig(channelsFile);

        this.messagesConfig = new MessagesConfig(this, mainConfig);
    }

    private void warnIfUnsupportedStorage() {
        String type = mainConfig.storageType();
        if (!"yaml".equalsIgnoreCase(type)) {
            getLogger().warning("storage.type '" + type + "' is not implemented in this build yet "
                    + "(planned for Phase 4); falling back to the YAML file backend.");
        }
    }

    private void setupIntegrations() {
        PluginManager pm = getServer().getPluginManager();
        boolean luckPermsPresent = pm.getPlugin("LuckPerms") != null;
        boolean vaultPresent = pm.getPlugin("Vault") != null;
        boolean papiPresent = pm.getPlugin("PlaceholderAPI") != null;

        if (integrationEnabled("luckperms", () -> luckPermsPresent) && luckPermsPresent) {
            try {
                this.luckPermsHook = new LuckPermsHook();
                this.permissionsProvider = luckPermsHook;
            } catch (IllegalStateException | LinkageError e) {
                getLogger().log(Level.WARNING, "LuckPerms was detected but its API could not be hooked", e);
            }
        }
        if (permissionsProvider == null && integrationEnabled("vault", () -> vaultPresent) && vaultPresent) {
            permissionsProvider = VaultPermissionHook.tryCreate().<PermissionsProvider>map(hook -> hook).orElse(null);
        }
        if (permissionsProvider == null) {
            permissionsProvider = new NoopPermissionsProvider();
        }

        if (integrationEnabled("vault", () -> vaultPresent) && vaultPresent) {
            economyProvider = VaultEconomyHook.tryCreate().<EconomyProvider>map(hook -> hook).orElseGet(NoopEconomyProvider::new);
        } else {
            economyProvider = new NoopEconomyProvider();
        }

        if (integrationEnabled("placeholderapi", () -> papiPresent) && papiPresent) {
            placeholderProvider = new PapiPlaceholderProvider();
        } else {
            placeholderProvider = new NoopPlaceholderProvider();
        }
    }

    private boolean integrationEnabled(String key, BooleanSupplier pluginPresent) {
        return mainConfig.integrationEnabled(key, pluginPresent);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ChatListener(scheduler, channelManager, profileCache, colorResolver, formatEngine,
                antiSpamService, swearFilterService, ignoreService, chatLogService, chatPauseState,
                mainConfig, messagesConfig, economyProvider), this);
        pm.registerEvents(new JoinQuitListener(playerDataStore, profileCache, channelManager, antiSpamService), this);

        if (luckPermsHook != null) {
            new PermissionChangeListener(this, luckPermsHook).register();
        }
    }

    private void registerCommands() {
        CommandRegistrar registrar = new CommandRegistrar(channelManager, profileCache, ignoreService, spyService,
                muteService, colorPermissionPolicy, mainConfig, messagesConfig, formatEngine, chatPauseState,
                this::reload, this::debugLines);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> registrar.registerAll(event.registrar()));
    }

    private void registerPlaceholderExpansionIfPresent() {
        if (placeholderProvider.isEnabled() && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion = new UniChatExpansion(channelManager, colorResolver, profileCache,
                    getDescription().getVersion());
            placeholderExpansion.register();
        }
    }

    /** Reloads {@code config.yml}/{@code channels.yml}/{@code messages_*.yml} in place; used by {@code /unichat reload}. */
    public void reload() {
        try {
            mainConfig.reload();
            channelsConfig.reloadFromDisk();
            messagesConfig.reload();
            channelManager.load(channelsConfig);
            swearFilterService.reload();
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to reload LINC-Chat configuration", e);
        }
    }

    private List<String> debugLines() {
        List<String> lines = new ArrayList<>();
        lines.add("Storage backend: " + mainConfig.storageType());
        lines.add("Permissions provider: " + permissionsProvider.name());
        lines.add("Economy integration: " + economyProvider.isEnabled());
        lines.add("PlaceholderAPI integration: " + placeholderProvider.isEnabled());
        lines.add("Channels loaded: " + channelManager.all().size());
        lines.add("Cached player profiles: " + profileCache.all().size());
        lines.add("Chat paused: " + chatPauseState.isPaused() + ", slowmode: " + chatPauseState.slowModeSeconds() + "s");
        return lines;
    }
}
