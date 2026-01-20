package me.hsgamer.topper.template.topplayernumber;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import me.hsgamer.topper.template.topplayernumber.manager.*;
import me.hsgamer.topper.value.core.ValueProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class TopPlayerNumberTemplate {
    private final Settings settings;
    private final TopManager topManager;
    private final TopQueryManager topQueryManager;
    private final EntryConsumeManager entryConsumeManager;
    private final QueryForwardManager queryForwardManager;
    private final NameProviderManager nameProviderManager;
    private final ReloadManager reloadManager;

    protected TopPlayerNumberTemplate(Settings settings) {
        this.settings = settings;
        this.topManager = new TopManager(this);
        this.topQueryManager = new TopQueryManager(this);
        this.entryConsumeManager = new EntryConsumeManager(this);
        this.queryForwardManager = new QueryForwardManager(this);
        this.nameProviderManager = new NameProviderManager();
        this.reloadManager = new ReloadManager();
    }

    public abstract Function<String, DataStorage<UUID, Double>> getStorageSupplier();

    public abstract Optional<ValueProvider<UUID, Double>> createValueProvider(Map<String, Object> settings);

    public abstract Agent createTask(Runnable runnable, NumberTopHolder.TaskType taskType, Map<String, Object> settings);

    public abstract void logWarning(String message, @Nullable Throwable throwable);

    public String getName(UUID uuid) {
        return this.nameProviderManager.getName(uuid);
    }

    public void logWarning(String message) {
        logWarning(message, null);
    }

    public void modifyAgents(NumberTopHolder holder, List<Agent> agents, List<DataEntryAgent<UUID, Double>> entryAgents) {

    }

    public void enable() {
        topManager.enable();
        entryConsumeManager.enable();
        queryForwardManager.enable();
    }

    public void disable() {
        queryForwardManager.disable();
        entryConsumeManager.disable();
        topManager.disable();
        reloadManager.clear();
    }

    public void reload() {
        reloadManager.call(ReloadManager.ReloadEntry::beforeReload);
        topManager.disable();
        reloadManager.call(ReloadManager.ReloadEntry::reload);
        topManager.enable();
        reloadManager.call(ReloadManager.ReloadEntry::afterReload);
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

    public QueryForwardManager getQueryForwardManager() {
        return queryForwardManager;
    }

    public NameProviderManager getNameProviderManager() {
        return nameProviderManager;
    }

    public ReloadManager getReloadManager() {
        return reloadManager;
    }

    public interface Settings {
        Map<String, NumberTopHolder.Settings> holders();

        int taskSaveEntryPerTick();

        int taskUpdateEntryPerTick();

        int taskUpdateMaxSkips();
    }
}
