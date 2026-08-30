package com.leidcool.lincchat.integration.luckperms;

import com.leidcool.lincchat.integration.PermissionsProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Direct LuckPerms API integration (TOR section 5.1): reads prefix/suffix and the plugin's
 * own meta keys ({@code unichat-prefix-color}, {@code unichat-name-color},
 * {@code unichat-message-color}, {@code unichat-local-radius}) without going through
 * PlaceholderAPI, for performance and so that meta changes can be observed live via
 * {@code UserDataRecalculateEvent} (subscribed to in {@code LincChatPlugin} /
 * {@code PermissionChangeListener}).
 */
public final class LuckPermsHook implements PermissionsProvider {

    private final LuckPerms luckPerms;

    public LuckPermsHook() {
        this.luckPerms = LuckPermsProvider.get();
    }

    public LuckPerms api() {
        return luckPerms;
    }

    private Optional<User> user(Player player) {
        return Optional.ofNullable(luckPerms.getUserManager().getUser(player.getUniqueId()));
    }

    private Optional<CachedMetaData> metaData(Player player) {
        return user(player).map(u -> u.getCachedData().getMetaData());
    }

    @Override
    public String name() {
        return "LuckPerms";
    }

    @Override
    public Optional<String> getPrefix(Player player) {
        return metaData(player).map(CachedMetaData::getPrefix).filter(s -> !s.isEmpty());
    }

    @Override
    public Optional<String> getSuffix(Player player) {
        return metaData(player).map(CachedMetaData::getSuffix).filter(s -> !s.isEmpty());
    }

    @Override
    public Optional<String> getPrimaryGroupDisplayName(Player player) {
        return user(player).map(u -> {
            Group group = luckPerms.getGroupManager().getGroup(u.getPrimaryGroup());
            if (group == null) {
                return u.getPrimaryGroup();
            }
            String display = group.getCachedData().getMetaData().getMetaValue("displayname");
            return display != null ? display : group.getName();
        });
    }

    @Override
    public Optional<String> getMeta(Player player, String key) {
        return metaData(player).map(meta -> meta.getMetaValue(key));
    }
}
