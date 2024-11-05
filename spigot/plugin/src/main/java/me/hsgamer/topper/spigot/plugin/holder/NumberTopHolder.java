package me.hsgamer.topper.spigot.plugin.holder;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.holder.AgentDataHolder;
import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.agent.storage.StorageAgent;
import me.hsgamer.topper.agent.update.UpdateAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.spigot.agent.runnable.SpigotRunnableAgent;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.config.MainConfig;
import me.hsgamer.topper.spigot.plugin.event.GenericEntryUpdateEvent;
import me.hsgamer.topper.spigot.plugin.holder.display.ValueDisplay;
import me.hsgamer.topper.spigot.plugin.holder.provider.ValueProvider;
import me.hsgamer.topper.spigot.plugin.manager.TopManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NumberTopHolder extends AgentDataHolder<UUID, Double> {
    private final ValueProvider valueProvider;
    private final ValueDisplay valueDisplay;
    private final UpdateAgent<UUID, Double> updateAgent;
    private final StorageAgent<UUID, Double> storageAgent;
    private final SnapshotAgent<UUID, Double> snapshotAgent;

    public NumberTopHolder(TopperPlugin instance, String name, Map<String, Object> map) {
        super(name);
        this.valueProvider = instance.get(ValueProviderBuilder.class).build(map).orElseGet(() -> {
            instance.getLogger().warning("No value provider found for " + name);
            return ValueProvider.EMPTY;
        });
        this.valueDisplay = new ValueDisplay(map);

        this.updateAgent = new UpdateAgent<>(instance.getLogger(), this, valueProvider::getValue);
        updateAgent.setMaxEntryPerCall(instance.get(MainConfig.class).getTaskUpdateEntryPerTick());
        addAgent(new SpigotRunnableAgent<>(updateAgent, AsyncScheduler.get(instance), instance.get(MainConfig.class).getTaskUpdateDelay()));

        this.storageAgent = new StorageAgent<>(instance.getLogger(), this, instance.get(TopManager.class).getStorageSupplier().getStorage(name));
        storageAgent.setMaxEntryPerCall(instance.get(MainConfig.class).getTaskSaveEntryPerTick());
        addAgent(new SpigotRunnableAgent<>(storageAgent, AsyncScheduler.get(instance), instance.get(MainConfig.class).getTaskSaveDelay()));

        this.snapshotAgent = new SnapshotAgent<>(this);
        boolean reverseOrder = Optional.ofNullable(map.get("reverse")).map(String::valueOf).map(Boolean::parseBoolean).orElse(true);
        snapshotAgent.setComparator(reverseOrder ? Comparator.reverseOrder() : Comparator.naturalOrder());
        addAgent(new SpigotRunnableAgent<>(snapshotAgent, AsyncScheduler.get(instance), 20L));

        addAgent(new Agent<UUID, Double>() {
            @Override
            public void start() {
                if (instance.get(MainConfig.class).isLoadAllOfflinePlayers()) {
                    GlobalScheduler.get(instance).run(() -> {
                        for (OfflinePlayer player : instance.getServer().getOfflinePlayers()) {
                            getOrCreateEntry(player.getUniqueId());
                        }
                    });
                }
            }

            @Override
            public void onUpdate(DataEntry<UUID, Double> entry) {
                Bukkit.getPluginManager().callEvent(new GenericEntryUpdateEvent(name, entry.getKey(), entry.getValue(), true));
            }
        });
    }

    @Override
    public Double getDefaultValue() {
        return valueProvider.getDefaultValue();
    }

    public UpdateAgent<UUID, Double> getUpdateAgent() {
        return updateAgent;
    }

    public StorageAgent<UUID, Double> getStorageAgent() {
        return storageAgent;
    }

    public SnapshotAgent<UUID, Double> getSnapshotAgent() {
        return snapshotAgent;
    }

    public ValueDisplay getValueDisplay() {
        return valueDisplay;
    }
}
