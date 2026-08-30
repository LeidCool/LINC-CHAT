package com.leidcool.lincchat.integration;

/**
 * Opaque handle to a scheduled/repeating task, returned by {@link SchedulerProvider}.
 */
public interface ScheduledTaskHandle {

    void cancel();
}
