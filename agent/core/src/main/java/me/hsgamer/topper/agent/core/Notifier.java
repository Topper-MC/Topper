package me.hsgamer.topper.agent.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class Notifier<E> {
    private final List<Consumer<E>> listeners = new ArrayList<>();

    public Runnable addListener(Consumer<E> listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    public void fire(E event) {
        fire(event, false);
    }

    public void fireReverse(E event) {
        fire(event, true);
    }

    private void fire(E event, boolean reverse) {
        Objects.requireNonNull(event);
        if (reverse) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
                listeners.get(i).accept(event);
            }
        } else {
            for (Consumer<E> listener : listeners) {
                listener.accept(event);
            }
        }
    }
}
