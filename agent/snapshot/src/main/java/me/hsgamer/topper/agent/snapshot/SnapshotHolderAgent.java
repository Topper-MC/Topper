package me.hsgamer.topper.agent.snapshot;

import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SnapshotHolderAgent<K, V> extends SnapshotAgent<K, V> implements DataEntryAgent<K, V> {
    private final DataHolder<K, V> holder;
    private final AtomicBoolean needUpdating = new AtomicBoolean(true);
    private Predicate<DataEntry<K, V>> dataFilter = null;

    public SnapshotHolderAgent(DataHolder<K, V> holder) {
        this.holder = holder;
    }

    public void setDataFilter(Predicate<DataEntry<K, V>> dataFilter) {
        this.dataFilter = dataFilter;
    }

    @Override
    protected Stream<Map.Entry<K, V>> getDataStream() {
        Stream<Map.Entry<K, DataEntry<K, V>>> stream = holder.getEntryMap().entrySet().stream();
        if (dataFilter != null) {
            stream = stream.filter(entry -> dataFilter.test(entry.getValue()));
        }
        return stream.map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getValue()));
    }

    @Override
    protected boolean needUpdate() {
        if (needUpdating.get()) {
            needUpdating.set(false);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate(DataEntry<K, V> entry) {
        needUpdating.set(true);
    }

    @Override
    public void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
        needUpdating.set(true);
    }

    @Override
    public void onRemove(DataEntry<K, V> entry) {
        needUpdating.set(true);
    }
}
