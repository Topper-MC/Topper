package me.hsgamer.topper.spigot.plugin.holder;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.hscore.common.CollectionUtils;
import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.agent.holder.AgentDataHolder;
import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.agent.storage.StorageAgent;
import me.hsgamer.topper.agent.update.UpdateAgent;
import me.hsgamer.topper.core.DataEntry;
import me.hsgamer.topper.spigot.agent.runnable.SpigotRunnableAgent;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.config.MainConfig;
import me.hsgamer.topper.spigot.plugin.holder.display.ValueDisplay;
import me.hsgamer.topper.spigot.plugin.manager.EntryConsumeManager;
import me.hsgamer.topper.spigot.plugin.manager.TopManager;
import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class NumberTopHolder extends AgentDataHolder<UUID, Double> {
    private final ValueDisplay valueDisplay;
    private final StorageAgent<UUID, Double> storageAgent;
    private final UpdateAgent<UUID, Double> updateAgent;
    private final SnapshotAgent<UUID, Double> snapshotAgent;

    public NumberTopHolder(TopperPlugin instance, String name, Map<String, Object> map) {
        super(name);
        this.valueDisplay = new ValueDisplay(map);

        this.storageAgent = new StorageAgent<>(this, instance.get(TopManager.class).buildStorage(name));
        storageAgent.setMaxEntryPerCall(instance.get(MainConfig.class).getTaskSaveEntryPerTick());
        addAgent(storageAgent);
        addEntryAgent(storageAgent);
        addAgent(new SpigotRunnableAgent(storageAgent, AsyncScheduler.get(instance), instance.get(MainConfig.class).getTaskSaveDelay()));

        ValueProvider<UUID, Double> valueProvider = instance.get(ValueProviderBuilder.class).build(map).orElseGet(() -> {
            instance.getLogger().warning("No value provider found for " + name);
            return ValueProvider.empty();
        });
        boolean isAsync = Optional.ofNullable(map.get("async"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        boolean showErrors = Optional.ofNullable(map.get("show-errors"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        List<String> ignorePermissions = CollectionUtils.createStringListFromObject(map.get("ignore-permission"), true);
        List<String> resetPermissions = CollectionUtils.createStringListFromObject(map.get("reset-permission"), true);
        this.updateAgent = new UpdateAgent<>(this, valueProvider);
        if (!ignorePermissions.isEmpty() || !resetPermissions.isEmpty()) {
            updateAgent.setFilter(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    return UpdateAgent.FilterResult.SKIP;
                }
                if (!resetPermissions.isEmpty() && resetPermissions.stream().anyMatch(player::hasPermission)) {
                    return UpdateAgent.FilterResult.RESET;
                }
                if (!ignorePermissions.isEmpty() && ignorePermissions.stream().anyMatch(player::hasPermission)) {
                    return UpdateAgent.FilterResult.SKIP;
                }
                return UpdateAgent.FilterResult.CONTINUE;
            });
        }
        if (showErrors) {
            updateAgent.setErrorHandler((uuid, valueWrapper) -> {
                if (valueWrapper.state == ValueWrapper.State.ERROR) {
                    instance.getLogger().log(Level.WARNING, "Error on getting value for " + name + " from " + uuid + " - " + valueWrapper.errorMessage, valueWrapper.throwable);
                }
            });
        }
        updateAgent.setMaxSkips(instance.get(MainConfig.class).getTaskUpdateMaxSkips());
        addEntryAgent(updateAgent);
        addAgent(new SpigotRunnableAgent(updateAgent.getUpdateRunnable(instance.get(MainConfig.class).getTaskUpdateEntryPerTick()), isAsync ? AsyncScheduler.get(instance) : GlobalScheduler.get(instance), instance.get(MainConfig.class).getTaskUpdateDelay()));
        addAgent(new SpigotRunnableAgent(updateAgent.getSetRunnable(), AsyncScheduler.get(instance), instance.get(MainConfig.class).getTaskUpdateSetDelay()));

        this.snapshotAgent = SnapshotAgent.create(this);
        boolean reverseOrder = Optional.ofNullable(map.get("reverse")).map(String::valueOf).map(Boolean::parseBoolean).orElse(false);
        snapshotAgent.setComparator(reverseOrder ? Comparator.naturalOrder() : Comparator.reverseOrder());
        addAgent(snapshotAgent);
        addAgent(new SpigotRunnableAgent(snapshotAgent, AsyncScheduler.get(instance), 20L));

        addAgent(new Agent() {
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
        });
        addEntryAgent(new DataEntryAgent<UUID, Double>() {
            @Override
            public void onUpdate(DataEntry<UUID, Double> entry, Double oldValue, Double newValue) {
                instance.get(EntryConsumeManager.class).consume(new EntryConsumeManager.Context(
                        TopperPlugin.GROUP,
                        name,
                        entry.getKey(),
                        oldValue,
                        newValue
                ));
            }
        });
    }

    @Override
    public Double getDefaultValue() {
        return 0D;
    }

    public StorageAgent<UUID, Double> getStorageAgent() {
        return storageAgent;
    }

    public UpdateAgent<UUID, Double> getUpdateAgent() {
        return updateAgent;
    }

    public SnapshotAgent<UUID, Double> getSnapshotAgent() {
        return snapshotAgent;
    }

    public ValueDisplay getValueDisplay() {
        return valueDisplay;
    }
}
