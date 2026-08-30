package com.leidcool.lincchat.channel.impl;

import com.leidcool.lincchat.channel.ChannelContext;
import com.leidcool.lincchat.config.ChannelDefinition;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Shared recipient-resolution logic for range-limited channels (Local, and Trade/Custom
 * channels that opt into a range). Implements TOR sections 6.3 and 16:
 * <ul>
 *     <li>per-viewer radius override via the {@code unichat-local-radius} LuckPerms meta key
 *     or a {@code unichat.local.range.<N>} permission node -- the override widens how far
 *     that viewer can "hear", it does not change how far the sender's voice "travels";</li>
 *     <li>{@code vertical-check} toggles 3D vs horizontal-only (2D) distance;</li>
 *     <li>recipients are narrowed using {@link World#getNearbyEntities} (chunk-based) instead
 *     of a naive distance check against every online player.</li>
 * </ul>
 */
public final class LocalRadiusResolver {

    private static final String RANGE_PERMISSION_PREFIX = "unichat.local.range.";
    private static final String RADIUS_META_KEY = "unichat-local-radius";

    private LocalRadiusResolver() {
    }

    /** @return the effective radius for {@code viewer}, or {@code <= 0} meaning unlimited. */
    public static int effectiveRadius(Player viewer, ChannelDefinition definition, ChannelContext context) {
        int base = definition.range();
        if (base <= 0) {
            return 0;
        }

        int effective = base;

        int permissionRadius = highestPermissionRadius(viewer);
        if (permissionRadius == 0) {
            return 0;
        }
        if (permissionRadius > effective) {
            effective = permissionRadius;
        }

        Optional<String> meta = context.permissions().getMeta(viewer, RADIUS_META_KEY);
        if (meta.isPresent()) {
            try {
                int metaValue = Integer.parseInt(meta.get().trim());
                if (metaValue <= 0) {
                    return 0;
                }
                if (metaValue > effective) {
                    effective = metaValue;
                }
            } catch (NumberFormatException ignored) {
                // malformed meta value, ignore and keep the permission/config-derived radius
            }
        }

        return effective;
    }

    private static int highestPermissionRadius(Player player) {
        int max = -1;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String permission = info.getPermission();
            if (!permission.startsWith(RANGE_PERMISSION_PREFIX)) {
                continue;
            }
            String suffix = permission.substring(RANGE_PERMISSION_PREFIX.length());
            try {
                int value = Integer.parseInt(suffix);
                if (value <= 0) {
                    return 0;
                }
                if (value > max) {
                    max = value;
                }
            } catch (NumberFormatException ignored) {
                // not a numeric suffix, not one of our nodes
            }
        }
        return max;
    }

    public static List<Player> nearby(Player sender, ChannelDefinition definition, ChannelContext context) {
        World world = sender.getWorld();

        if (definition.range() <= 0) {
            return new ArrayList<>(world.getPlayers());
        }

        int maxRadiusNeeded = 0;
        boolean anyUnlimited = false;
        for (Player candidate : world.getPlayers()) {
            int radius = effectiveRadius(candidate, definition, context);
            if (radius <= 0) {
                anyUnlimited = true;
                break;
            }
            if (radius > maxRadiusNeeded) {
                maxRadiusNeeded = radius;
            }
        }
        if (anyUnlimited) {
            return new ArrayList<>(world.getPlayers());
        }

        boolean verticalCheck = definition.verticalCheck();
        double horizontal = maxRadiusNeeded;
        double vertical = verticalCheck ? maxRadiusNeeded : (world.getMaxHeight() - world.getMinHeight());

        Location senderLocation = sender.getLocation();
        Collection<Entity> nearbyEntities = world.getNearbyEntities(senderLocation, horizontal, vertical, horizontal,
                entity -> entity instanceof Player);

        List<Player> result = new ArrayList<>();
        result.add(sender);
        for (Entity entity : nearbyEntities) {
            Player candidate = (Player) entity;
            if (candidate.equals(sender)) {
                continue;
            }
            int radius = effectiveRadius(candidate, definition, context);
            double distanceSquared = verticalCheck
                    ? candidate.getLocation().distanceSquared(senderLocation)
                    : horizontalDistanceSquared(candidate.getLocation(), senderLocation);
            double limit = radius <= 0 ? Double.MAX_VALUE : (double) radius * (double) radius;
            if (distanceSquared <= limit) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
