package com.leidcool.lincchat.integration;

import org.bukkit.entity.Entity;

/**
 * Abstraction over the server scheduler. On regular Paper this simply wraps the Bukkit
 * scheduler; on Folia a future implementation would dispatch to the global/region/entity
 * schedulers instead. Business logic must never call {@code Bukkit.getScheduler()} directly
 * (see TOR section 16) so that swapping the implementation is the only change needed to
 * become Folia-compatible.
 */
public interface SchedulerProvider {

    /** Runs the task asynchronously, off the main/region thread. */
    void runAsync(Runnable task);

    /** Runs the task on the global/main tick thread as soon as possible. */
    void runGlobalSync(Runnable task);

    /**
     * Runs the task on the thread that owns the given entity (the main thread on Paper,
     * the entity's region thread on Folia). {@code retired} runs instead if the entity is
     * removed before the task executes.
     */
    void runEntityTask(Entity entity, Runnable task, Runnable retired);

    /** Schedules a one-off task on the global sync thread after {@code delayTicks}. */
    ScheduledTaskHandle runGlobalSyncLater(Runnable task, long delayTicks);

    /** Schedules a repeating task on the global sync thread. */
    ScheduledTaskHandle runGlobalSyncRepeating(Runnable task, long delayTicks, long periodTicks);

    /** Schedules a repeating task off the main thread. */
    ScheduledTaskHandle runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);
}
