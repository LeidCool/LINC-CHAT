package com.leidcool.lincchat.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Interface for resolving localized, MiniMessage-formatted messages by key. Split out from
 * {@link MessagesConfig} so that a future per-player locale feature (TOR section 15:
 * {@code unichat-locale} LP meta / {@code /lang}) only needs a different implementation of
 * this interface, without touching every call site that currently calls
 * {@code messages().get(key, ...)} with the server-wide default locale.
 */
public interface MessagesProvider {

    Component get(String locale, String key, TagResolver... resolvers);

    default Component get(String key, TagResolver... resolvers) {
        return get(defaultLocale(), key, resolvers);
    }

    String defaultLocale();
}
