package me.hsgamer.topper.agent.update;

import me.hsgamer.hscore.logger.common.LogLevel;
import me.hsgamer.hscore.logger.common.Logger;
import me.hsgamer.hscore.logger.provider.LoggerProvider;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.core.DataHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

public class UpdateAgent<K, V> implements DataEntryAgent<K, V>, Runnable {
    private static final Logger LOGGER = LoggerProvider.getLogger(UpdateAgent.class);

    private final Queue<K> updateQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final DataHolder<K, V> holder;
    private final Function<K, CompletableFuture<Optional<V>>> updateFunction;
    private final List<Predicate<K>> filters = new ArrayList<>();
    private int maxEntryPerCall = 10;

    public UpdateAgent(DataHolder<K, V> holder, Function<K, CompletableFuture<Optional<V>>> updateFunction) {
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
        if (!updating.compareAndSet(false, true)) return;

        List<K> keys = new ArrayList<>(maxEntryPerCall);
        for (int i = 0; i < maxEntryPerCall; i++) {
            K k = updateQueue.poll();
            if (k == null) break;
            if (!canUpdate(k)) continue;
            keys.add(k);
        }

        if (keys.isEmpty()) {
            updating.set(false);
            return;
        }

        CompletableFuture
                .allOf(
                        keys.stream()
                                .map(k -> {
                                    DataEntry<K, V> entry = holder.getOrCreateEntry(k);
                                    return updateFunction.apply(k)
                                            .thenAcceptAsync(optional -> optional.ifPresent(entry::setValue))
                                            .whenComplete((v, throwable) -> {
                                                if (throwable != null) {
                                                    LOGGER.log(LogLevel.ERROR, "An error occurred while updating the entry: " + k, throwable);
                                                }
                                            });
                                })
                                .toArray(CompletableFuture[]::new)
                )
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        LOGGER.log(LogLevel.ERROR, "An error occurred while updating the entries", throwable);
                    }
                    updateQueue.addAll(keys);
                    updating.set(false);
                });
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
