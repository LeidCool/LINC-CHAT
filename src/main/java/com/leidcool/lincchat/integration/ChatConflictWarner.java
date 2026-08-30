package com.leidcool.lincchat.integration;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Other chat-format plugins will keep rendering vanilla/Essentials-style chat unless they are
 * disabled. LINC-Chat cancels {@code AsyncChatEvent} and delivers messages itself, but a
 * competing listener at {@code HIGHEST} (or the legacy {@code AsyncPlayerChatEvent} path used
 * by EssentialsChat) can still win if LINC-Chat never loaded, or can duplicate/override output.
 */
public final class ChatConflictWarner {

    private static final List<String> COMPETING_CHAT_PLUGINS = List.of(
            "EssentialsChat",
            "EZColors",
            "EmotesChat",
            "VentureChat",
            "ChatManager",
            "ChatControl",
            "Chatty",
            "LegendaryChat",
            "AdvancedChat"
    );

    /** Plugins that often steal {@code !} / {@code $} as their own chat symbols. */
    private static final List<String> SHORTCUT_STEALERS = List.of("hChatGame");

    private ChatConflictWarner() {
    }

    public static void warnIfPresent(Plugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        Logger log = plugin.getLogger();
        List<String> found = new ArrayList<>();
        for (String name : COMPETING_CHAT_PLUGINS) {
            if (pm.getPlugin(name) != null) {
                found.add(name);
            }
        }
        if (!found.isEmpty()) {
            log.warning("Other chat plugins are enabled and will fight LINC-Chat for the chat format: "
                    + String.join(", ", found) + ". Remove or disable them (keep Essentials itself; "
                    + "only EssentialsChat must go), then restart the server.");
        }
        List<String> stealers = new ArrayList<>();
        for (String name : SHORTCUT_STEALERS) {
            if (pm.getPlugin(name) != null) {
                stealers.add(name);
            }
        }
        if (!stealers.isEmpty()) {
            log.warning(String.join(", ", stealers)
                    + " is enabled and often uses '!' as its own chat symbol. LINC-Chat now "
                    + "captures chat first, but if global '!' still goes to local, disable that "
                    + "plugin's chat-symbol / global-prefix feature (keep minigames if you want).");
        }
    }
}
