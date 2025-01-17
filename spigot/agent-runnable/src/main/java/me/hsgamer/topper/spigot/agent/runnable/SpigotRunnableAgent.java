package me.hsgamer.topper.spigot.agent.runnable;

import io.github.projectunified.minelib.scheduler.common.scheduler.Scheduler;
import io.github.projectunified.minelib.scheduler.common.task.Task;
import me.hsgamer.topper.agent.runnable.RunnableAgent;

public class SpigotRunnableAgent extends RunnableAgent {
    private final Scheduler scheduler;
    private final long interval;

    public SpigotRunnableAgent(Runnable runnable, Scheduler scheduler, long interval) {
        super(runnable);
        this.scheduler = scheduler;
        this.interval = interval;
    }

    @Override
    protected Runnable run(Runnable runnable) {
        Task task = scheduler.runTimer(runnable, interval, interval);
        return task::cancel;
    }
}
