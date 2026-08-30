package com.leidcool.lincchat.channel;

import com.leidcool.lincchat.integration.PermissionsProvider;

/**
 * Bundles the integration services that {@code Channel} implementations need to resolve
 * recipients (currently just {@link PermissionsProvider}, for the local-radius LuckPerms
 * meta override), without giving every channel implementation a constructor dependency on
 * the whole service graph.
 */
public final class ChannelContext {

    private final PermissionsProvider permissions;

    public ChannelContext(PermissionsProvider permissions) {
        this.permissions = permissions;
    }

    public PermissionsProvider permissions() {
        return permissions;
    }
}
