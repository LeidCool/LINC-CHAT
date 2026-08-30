package com.leidcool.lincchat.api;

import com.leidcool.lincchat.channel.ChannelType;

/**
 * Public, read-only view of a chat channel, exposed to third-party plugins via
 * {@link UniChatAPI}. The full internal model lives in {@code com.leidcool.lincchat.channel.Channel}.
 */
public interface ChatChannel {

    String id();

    String displayName();

    boolean isEnabled();

    ChannelType type();

    /** Raw MiniMessage tag template shown before the message, e.g. {@code "[G]"}. */
    String tag();
}
