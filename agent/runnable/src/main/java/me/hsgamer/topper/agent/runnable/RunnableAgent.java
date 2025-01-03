package me.hsgamer.topper.agent.runnable;

import me.hsgamer.topper.agent.core.Agent;

public abstract class RunnableAgent implements Agent {
    private final Runnable runnable;
    private Runnable cancelTaskRunnable;

    public RunnableAgent(Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * This method is used to run the task and return a {@link Runnable} to cancel it
     *
     * @param runnable the task to run
     * @return the {@link Runnable} to cancel the task
     */
    protected abstract Runnable run(Runnable runnable);

    @Override
    public void start() {
        cancelTaskRunnable = run(runnable);
    }

    @Override
    public void stop() {
        if (cancelTaskRunnable != null) {
            cancelTaskRunnable.run();
        }
    }
}
