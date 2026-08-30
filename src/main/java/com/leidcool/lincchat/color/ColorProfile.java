package com.leidcool.lincchat.color;

/**
 * Resolved MiniMessage colour tags for the three independent chat colour layers
 * (TOR section 8): prefix, nickname and message text.
 */
public record ColorProfile(String prefixColor, String nameColor, String messageColor) {
}
