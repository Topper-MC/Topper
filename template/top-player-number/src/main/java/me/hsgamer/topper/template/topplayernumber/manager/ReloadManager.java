package me.hsgamer.topper.template.topplayernumber.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ReloadManager {
    private final List<ReloadEntry> reloadEntries = new ArrayList<>();

    public void add(ReloadEntry entry) {
        reloadEntries.add(entry);
    }

    public void clear() {
        reloadEntries.clear();
    }

    public void call(Consumer<ReloadEntry> consumer) {
        for (ReloadEntry entry : reloadEntries) {
            consumer.accept(entry);
        }
    }

    public interface ReloadEntry {
        default void beforeReload() {
            // EMPTY
        }

        default void reload() {
            // EMPTY
        }

        default void afterReload() {
            // EMPTY
        }
    }
}
