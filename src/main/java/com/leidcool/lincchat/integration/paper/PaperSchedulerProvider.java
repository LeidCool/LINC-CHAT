package com.leidcool.lincchat.integration.paper;

import com.leidcool.lincchat.integration.ScheduledTaskHandle;
import com.leidcool.lincchat.integration.SchedulerProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Regular Paper implementation of {@link SchedulerProvider}, backed by the Bukkit scheduler.
 * <p>
 * TODO (Phase 4 / Folia): replace the bodies of this class with calls to
 * {@code Bukkit.getGlobalRegionScheduler()}, {@code Bukkit.getAsyncScheduler()} and
 * {@code entity.getScheduler()} when Folia support is implemented. Because all call sites
 * already depend on {@link SchedulerProvider} rather than the Bukkit scheduler directly, this
 * is expected to be a self-contained change.
 */
public final class PaperSchedulerProvider implements SchedulerProvider {

    private final Plugin plugin;

    public PaperSchedulerProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runGlobalSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public void runEntityTask(Entity entity, Runnable task, Runnable retired) {
        // On regular Paper there is only one main thread; the entity may already have been
        // removed by the time the task runs, so we defensively re-check `isValid()`.
        runGlobalSync(() -> {
            if (entity.isValid()) {
                task.run();
            } else {
                retired.run();
            }
        });
    }

    @Override
    public ScheduledTaskHandle runGlobalSyncLater(Runnable task, long delayTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        return bukkitTask::cancel;
    }

    @Override
    public ScheduledTaskHandle runGlobalSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }

    @Override
    public ScheduledTaskHandle runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return bukkitTask::cancel;
    }
}
