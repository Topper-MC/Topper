package me.hsgamer.topper.agent.core;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.core.DataHolder;

public interface AgentHolder<K, V> extends DataHolder<K, V> {
    Notifier<EntryEvent<K, V>> getEntryNotifier();

    Notifier<HolderEvent> getHolderNotifier();

    @Override
    default void onCreate(DataEntry<K, V> entry) {
        Notifier<EntryEvent<K, V>> notifier = getEntryNotifier();
        if (notifier.isEmpty()) {
            return;
        }
        notifier.fire(new EntryEvent<>(EntryEvent.Kind.CREATED, entry, null, null));
    }

    @Override
    default void onRemove(DataEntry<K, V> entry) {
        Notifier<EntryEvent<K, V>> notifier = getEntryNotifier();
        if (notifier.isEmpty()) {
            return;
        }
        notifier.fire(new EntryEvent<>(EntryEvent.Kind.REMOVED, entry, null, null));
    }

    @Override
    default void onUpdate(DataEntry<K, V> entry, V oldValue, V newValue) {
        Notifier<EntryEvent<K, V>> notifier = getEntryNotifier();
        if (notifier.isEmpty()) {
            return;
        }
        notifier.fire(new EntryEvent<>(EntryEvent.Kind.UPDATED, entry, oldValue, newValue));
    }

    default void register() {
        Notifier<HolderEvent> notifier = getHolderNotifier();
        if (notifier.isEmpty()) {
            return;
        }
        notifier.fire(HolderEvent.REGISTERED);
    }

    default void unregister() {
        Notifier<HolderEvent> holderNotifier = getHolderNotifier();
        if (!holderNotifier.isEmpty()) {
            holderNotifier.fireReverse(HolderEvent.BEFORE_UNREGISTER);
        }

        if (!holderNotifier.isEmpty()) {
            holderNotifier.fireReverse(HolderEvent.UNREGISTERED);
        }
    }
}
