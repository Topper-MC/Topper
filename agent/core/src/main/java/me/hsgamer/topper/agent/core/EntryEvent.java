package me.hsgamer.topper.agent.core;

import me.hsgamer.topper.data.core.DataEntry;

public final class EntryEvent<K, V> {
    public final Kind kind;
    public final DataEntry<K, V> entry;
    public final V oldValue;
    public final V newValue;

    public EntryEvent(Kind kind, DataEntry<K, V> entry, V oldValue, V newValue) {
        this.kind = kind;
        this.entry = entry;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public enum Kind {
        CREATED,
        UPDATED,
        REMOVED
    }
}
