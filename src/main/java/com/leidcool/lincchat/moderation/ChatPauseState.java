package com.leidcool.lincchat.moderation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-wide chat freeze ({@code /chatpause}) and minimum-cooldown slowmode
 * ({@code /slowmode}), TOR section 11. Purely in-memory, does not survive a restart.
 */
public final class ChatPauseState {

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicInteger slowModeSeconds = new AtomicInteger(0);

    public boolean isPaused() {
        return paused.get();
    }

    public void setPaused(boolean value) {
        paused.set(value);
    }

    public int slowModeSeconds() {
        return slowModeSeconds.get();
    }

    public void setSlowModeSeconds(int seconds) {
        slowModeSeconds.set(Math.max(0, seconds));
    }

    public boolean isSlowModeActive() {
        return slowModeSeconds.get() > 0;
    }
}
