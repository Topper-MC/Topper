package me.hsgamer.topper.agent.update;

import me.hsgamer.topper.agent.core.AgentHolder;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;
import me.hsgamer.topper.value.core.ValueWrapper;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class UpdateAgent<K, V> {
    private static final int IDLE = 0;
    private static final int UPDATING = 1;
    private static final int RESULT = 2;
    private static final int RESET = 3;

    private final DataHolder<K, V> holder;
    private final BiConsumer<K, Consumer<ValueWrapper<V>>> updateConsumer;

    private final Map<K, EntryState<K, V>> map = new ConcurrentHashMap<>();

    private Function<K, FilterResult> filter = null;
    private BiFunction<K, ValueWrapper<V>, ValueWrapper<V>> errorHandler = null;
    private int maxSkips = 1;

    public UpdateAgent(DataHolder<K, V> holder, BiConsumer<K, Consumer<ValueWrapper<V>>> updateConsumer) {
        this.holder = holder;
        this.updateConsumer = updateConsumer;
    }

    public void setFilter(Function<K, FilterResult> filter) {
        this.filter = filter;
    }

    public void setErrorHandler(BiFunction<K, ValueWrapper<V>, ValueWrapper<V>> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setErrorHandler(BiConsumer<K, ValueWrapper<V>> errorHandler) {
        setErrorHandler((k, v) -> {
            errorHandler.accept(k, v);
            return v;
        });
    }

    public void setMaxSkips(int maxSkips) {
        this.maxSkips = maxSkips;
    }

    private void setValue(EntryState<K, V> state, V value) {
        Optional<DataEntry<K, V>> optionalEntry = holder.getEntry(state.key);
        if (!optionalEntry.isPresent()) {
            state.removed = true;
            return;
        }
        optionalEntry.get().setValue(value);
    }

    private void applyResult(EntryState<K, V> state, ValueWrapper<V> valueWrapper) {
        if (errorHandler != null && valueWrapper.state == ValueWrapper.State.ERROR) {
            valueWrapper = errorHandler.apply(state.key, valueWrapper);
        }
        if (valueWrapper.state == ValueWrapper.State.HANDLED) {
            setValue(state, valueWrapper.value);
        } else {
            state.skips.set(maxSkips);
        }
    }

    public Runnable getUpdateRunnable(int maxEntryPerCall) {
        return new Runnable() {
            private final ThreadLocal<Iterator<EntryState<K, V>>> iteratorRef = ThreadLocal.withInitial(() -> map.values().iterator());

            @Override
            public void run() {
                if (map.isEmpty()) {
                    return;
                }
                Iterator<EntryState<K, V>> iterator = iteratorRef.get();
                if (!iterator.hasNext()) {
                    iterator = map.values().iterator();
                    iteratorRef.set(iterator);
                }

                int count = 0;
                while (count < maxEntryPerCall && iterator.hasNext()) {
                    EntryState<K, V> state = iterator.next();
                    if (state.removed || state.skips.get() > 0) {
                        continue;
                    }

                    if (filter != null) {
                        switch (filter.apply(state.key)) {
                            case SKIP:
                                state.skips.set(maxSkips);
                                continue;
                            case RESET:
                                state.status.compareAndSet(IDLE, RESET);
                                continue;
                            default:
                                // Do nothing, continue to update
                                break;
                        }
                    }

                    if (state.status.compareAndSet(IDLE, UPDATING)) {
                        updateConsumer.accept(state.key, state);
                        count++;
                    }
                }
            }
        };
    }

    public Runnable getSetRunnable() {
        return () -> {
            for (EntryState<K, V> state : map.values()) {
                if (state.removed) {
                    continue;
                }
                int status = state.status.get();
                if (status == RESULT) {
                    if (!state.status.compareAndSet(RESULT, IDLE)) {
                        continue;
                    }
                    ValueWrapper<V> valueWrapper = state.result.getAndSet(null);
                    if (valueWrapper == null) {
                        continue;
                    }
                    applyResult(state, valueWrapper);
                } else if (status == RESET) {
                    if (!state.status.compareAndSet(RESET, IDLE)) {
                        continue;
                    }
                    setValue(state, null);
                    state.skips.set(maxSkips);
                } else if (status == IDLE && state.skips.get() > 0) {
                    state.skips.getAndUpdate(skips -> skips > 0 ? skips - 1 : 0);
                }
            }
        };
    }

    public void bindTo(AgentHolder<K, V> holder) {
        holder.getEntryNotifier().addListener(e -> {
            switch (e.kind) {
                case CREATED:
                    EntryState<K, V> state = new EntryState<>(e.entry.getKey());
                    EntryState<K, V> oldState = map.put(state.key, state);
                    if (oldState != null) {
                        oldState.removed = true;
                    }
                    break;
                case REMOVED:
                    EntryState<K, V> removedState = map.remove(e.entry.getKey());
                    if (removedState != null) {
                        removedState.removed = true;
                    }
                    break;
                default:
                    break;
            }
        });
    }

    public enum FilterResult {
        SKIP,
        RESET,
        CONTINUE
    }

    private static final class EntryState<K, V> implements Consumer<ValueWrapper<V>> {
        private final K key;
        private final AtomicInteger status = new AtomicInteger(IDLE);
        private final AtomicReference<ValueWrapper<V>> result = new AtomicReference<>();
        private final AtomicInteger skips = new AtomicInteger();
        private volatile boolean removed;

        private EntryState(K key) {
            this.key = key;
        }

        @Override
        public void accept(ValueWrapper<V> valueWrapper) {
            result.set(valueWrapper);
            status.set(RESULT);
        }
    }
}
