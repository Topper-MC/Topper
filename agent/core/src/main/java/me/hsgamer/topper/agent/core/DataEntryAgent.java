package me.hsgamer.topper.agent.core;

import me.hsgamer.topper.core.DataEntry;

public interface DataEntryAgent<K, V> {
    default void onCreate(DataEntry<K, V> entry) {
        // EMPTY
    }

    default void onUpdate(DataEntry<K, V> entry, V oldValue) {
        // EMPTY
    }

    default void onRemove(DataEntry<K, V> entry) {
        // EMPTY
    }

    default void onUnregister(DataEntry<K, V> entry) {
        // EMPTY
    }
}
