package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAgent<K, V> implements Agent<K, V>, Runnable {
    private final Logger logger;
    private final Queue<K> updateQueue = new ConcurrentLinkedQueue<>();
    private final DataHolder<K, V> holder;
    private final Function<K, CompletableFuture<Optional<V>>> updateFunction;
    private int maxEntryPerCall = 10;

    public UpdateAgent(Logger logger, DataHolder<K, V> holder, Function<K, CompletableFuture<Optional<V>>> updateFunction) {
        this.logger = logger;
        this.holder = holder;
        this.updateFunction = updateFunction;
    }

    public void setMaxEntryPerCall(int maxEntryPerCall) {
        this.maxEntryPerCall = maxEntryPerCall;
    }

    @Override
    public void run() {
        for (int i = 0; i < maxEntryPerCall; i++) {
            K k = updateQueue.poll();
            if (k == null) {
                break;
            }
            DataEntry<K, V> entry = holder.getOrCreateEntry(k);
            updateFunction.apply(k)
                    .thenAcceptAsync(optional -> optional.ifPresent(entry::setValue))
                    .whenComplete((v, throwable) -> {
                        if (throwable != null) {
                            logger.log(Level.WARNING, "An error occurred while updating the entry: " + k, throwable);
                        }
                        updateQueue.add(k);
                    });
        }
    }

    @Override
    public void onCreate(DataEntry<K, V> entry) {
        updateQueue.add(entry.getKey());
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        updateQueue.remove(entry.getKey());
    }
}
