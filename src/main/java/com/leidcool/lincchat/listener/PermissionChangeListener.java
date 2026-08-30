package com.leidcool.lincchat.listener;

import com.leidcool.lincchat.integration.luckperms.LuckPermsHook;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Subscribes to LuckPerms' own event bus (not Bukkit's) so meta/prefix/suffix changes (e.g.
 * an admin running {@code /lp user X meta set unichat-name-color ...}) are visible immediately.
 * <p>
 * {@link com.leidcool.lincchat.color.ColorResolver} already queries LuckPerms live on every
 * message, so there is no cache to invalidate here today; this class exists as the documented
 * extension point (TOR) for a future cached-colours optimisation, and for debug logging.
 */
public final class PermissionChangeListener {

    private final Plugin plugin;
    private final LuckPermsHook luckPermsHook;

    public PermissionChangeListener(Plugin plugin, LuckPermsHook luckPermsHook) {
        this.plugin = plugin;
        this.luckPermsHook = luckPermsHook;
    }

    public void register() {
        luckPermsHook.api().getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event ->
                plugin.getLogger().log(Level.FINE, () -> "LuckPerms data recalculated for " + event.getUser().getUniqueId()));
    }
}
