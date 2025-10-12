package me.hsgamer.topper.template.topplayernumber;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import me.hsgamer.topper.template.topplayernumber.manager.EntryConsumeManager;
import me.hsgamer.topper.template.topplayernumber.manager.TopManager;
import me.hsgamer.topper.template.topplayernumber.manager.TopQueryManager;
import me.hsgamer.topper.value.core.ValueProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class TopPlayerNumberTemplate {
    private final Settings settings;
    private final TopManager topManager;
    private final TopQueryManager topQueryManager;
    private final EntryConsumeManager entryConsumeManager;

    protected TopPlayerNumberTemplate(Settings settings) {
        this.settings = settings;
        this.topManager = new TopManager(this);
        this.topQueryManager = new TopQueryManager(this);
        this.entryConsumeManager = new EntryConsumeManager(this);
    }

    public abstract DataStorage<UUID, Double> getStorage(String name);

    public abstract Optional<ValueProvider<UUID, Double>> createValueProvider(Map<String, Object> settings);

    public abstract boolean isOnline(UUID uuid);

    public abstract String getName(UUID uuid);

    public abstract boolean hasPermission(UUID uuid, String permission);

    public abstract Agent createTaskAgent(Runnable runnable, boolean async, long delay);

    public abstract void logWarning(String message, @Nullable Throwable throwable);

    public void logWarning(String message) {
        logWarning(message, null);
    }

    public void modifyAgents(NumberTopHolder holder, List<Agent> agents, List<DataEntryAgent<UUID, Double>> entryAgents) {

    }

    public void enable() {
        topManager.enable();
        entryConsumeManager.enable();
    }

    public void disable() {
        entryConsumeManager.disable();
        topManager.disable();
    }

    public Settings getSettings() {
        return settings;
    }

    public TopManager getTopManager() {
        return topManager;
    }

    public TopQueryManager getTopQueryManager() {
        return topQueryManager;
    }

    public EntryConsumeManager getEntryConsumeManager() {
        return entryConsumeManager;
    }

    public interface Settings {
        Map<String, NumberTopHolder.Settings> holders();

        int taskSaveDelay();

        int taskSaveEntryPerTick();

        int taskUpdateEntryPerTick();

        int taskUpdateDelay();

        int taskUpdateSetDelay();

        int taskUpdateMaxSkips();
    }
}
