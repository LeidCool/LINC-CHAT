package com.leidcool.lincchat.channel;

/**
 * Broad category of a channel. Built-in ids ({@code global}/{@code local}/{@code trade}/
 * {@code admin}/{@code private}) map to their dedicated type; any other id configured in
 * {@code channels.yml} is treated as {@link #CUSTOM} (TOR section 6.6).
 */
public enum ChannelType {
    GLOBAL,
    LOCAL,
    TRADE,
    ADMIN,
    PRIVATE,
    CUSTOM;

    public static ChannelType fromId(String id) {
        return switch (id.toLowerCase(java.util.Locale.ROOT)) {
            case "global" -> GLOBAL;
            case "local" -> LOCAL;
            case "trade" -> TRADE;
            case "admin" -> ADMIN;
            case "private" -> PRIVATE;
            default -> CUSTOM;
        };
    }
}
