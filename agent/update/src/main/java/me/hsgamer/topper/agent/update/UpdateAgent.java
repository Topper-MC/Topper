package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V>, Runnable {
    private final Logger logger;
    private final Queue<K> updateQueue = new ConcurrentLinkedQueue<>();
    private final DataHolder<K, V> holder;
    private final Function<K, CompletableFuture<Optional<V>>> updateFunction;
    private final List<Predicate<K>> filters = new ArrayList<>();
    private int maxEntryPerCall = 10;

    public UpdateAgent(Logger logger, DataHolder<K, V> holder, Function<K, CompletableFuture<Optional<V>>> updateFunction) {
        this.logger = logger;
        this.holder = holder;
        this.updateFunction = updateFunction;
    }

    public void setMaxEntryPerCall(int maxEntryPerCall) {
        this.maxEntryPerCall = maxEntryPerCall;
    }

    public void addFilter(Predicate<K> filter) {
        filters.add(filter);
    }

    private boolean canUpdate(K key) {
        return filters.stream().allMatch(predicate -> predicate.test(key));
    }

    @Override
    public void run() {
        for (int i = 0; i < maxEntryPerCall; i++) {
            K k = updateQueue.poll();
            if (k == null) {
                break;
            }
            DataEntry<K, V> entry = holder.getOrCreateEntry(k);
            (canUpdate(k)
                    ? updateFunction.apply(k).thenAcceptAsync(optional -> optional.ifPresent(entry::setValue))
                    : CompletableFuture.completedFuture(null)
            )
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
