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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        Queue<Map.Entry<UUID, CompletableFuture<Optional<Double>>>> queue = new ConcurrentLinkedQueue<>();
        addAgent(new SpigotRunnableAgent(() -> {
            while (true) {
                Map.Entry<UUID, CompletableFuture<Optional<Double>>> entry = queue.poll();
                if (entry == null) {
                    break;
                }
                UUID uuid = entry.getKey();
                Optional<Double> value = valueProvider.apply(uuid).asOptional((errorMessage, throwable) -> {
                    if (showErrors) {
                        instance.getLogger().log(Level.WARNING, "Error on getting value for " + name + " from " + uuid + " - " + errorMessage, throwable);
                    }
                });
                entry.getValue().complete(value);
            }
        }, isAsync ? AsyncScheduler.get(instance) : GlobalScheduler.get(instance), instance.get(MainConfig.class).getTaskUpdateDelay()));
        this.updateAgent = new UpdateAgent<>(this, uuid -> {
            CompletableFuture<Optional<Double>> future = new CompletableFuture<>();
            return queue.offer(new AbstractMap.SimpleEntry<>(uuid, future)) ? future : CompletableFuture.completedFuture(Optional.empty());
        });
        updateAgent.setMaxEntryPerCall(instance.get(MainConfig.class).getTaskUpdateEntryPerTick());
        addEntryAgent(updateAgent);
        addAgent(new SpigotRunnableAgent(updateAgent, AsyncScheduler.get(instance), 0));
        List<String> ignorePermissions = CollectionUtils.createStringListFromObject(map.get("ignore-permission"), true);
        if (!ignorePermissions.isEmpty()) {
            updateAgent.addFilter(uuid -> {
                Player player = Bukkit.getPlayer(uuid);
                return player == null || ignorePermissions.stream().noneMatch(player::hasPermission);
            });
        }

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
