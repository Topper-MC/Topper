package me.hsgamer.topper.agent.core;

public interface Agent {
    default void start() {
        // EMPTY
    }

    default void stop() {
        // EMPTY
    }

    default void beforeStop() {
        // EMPTY
    }
}
