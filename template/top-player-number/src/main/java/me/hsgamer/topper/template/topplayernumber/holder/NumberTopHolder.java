package me.hsgamer.topper.template.topplayernumber.holder;

import me.hsgamer.topper.agent.core.Agent;
import me.hsgamer.topper.agent.core.AgentHolder;
import me.hsgamer.topper.agent.core.DataEntryAgent;
import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.agent.storage.StorageAgent;
import me.hsgamer.topper.agent.update.UpdateAgent;
import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.data.simple.SimpleDataHolder;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.display.ValueDisplay;
import me.hsgamer.topper.template.topplayernumber.manager.EntryConsumeManager;
import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NumberTopHolder extends SimpleDataHolder<UUID, Double> implements AgentHolder<UUID, Double> {
    public static final String GROUP = "topper";

    private final ValueDisplay valueDisplay;
    private final List<Agent> agents;
    private final List<DataEntryAgent<UUID, Double>> entryAgents;
    private final StorageAgent<UUID, Double> storageAgent;
    private final UpdateAgent<UUID, Double> updateAgent;
    private final SnapshotAgent<UUID, Double> snapshotAgent;
    private final Double defaultValue;

    public NumberTopHolder(TopPlayerNumberTemplate template, String name, Settings settings) {
        this.defaultValue = settings.defaultValue();

        List<Agent> agents = new ArrayList<>();
        List<DataEntryAgent<UUID, Double>> entryAgents = new ArrayList<>();
        this.valueDisplay = new ValueDisplay(template.getNameProviderManager()::getName, settings.displaySettings());

        this.storageAgent = new StorageAgent<>(template.getTopManager().buildStorage(name));
        storageAgent.setMaxEntryPerCall(template.getSettings().taskSaveEntryPerTick());
        agents.add(storageAgent);
        agents.add(storageAgent.getLoadAgent(this));
        agents.add(template.createTaskAgent(storageAgent, true, template.getSettings().taskSaveDelay()));
        entryAgents.add(storageAgent);

        ValueProvider<UUID, Double> valueProvider = template.createValueProvider(settings.valueProvider()).orElseGet(() -> {
            template.logWarning("No value provider found for " + name);
            return ValueProvider.empty();
        });
        boolean isAsync = settings.async();
        boolean showErrors = settings.showErrors();
        boolean resetOnError = settings.resetOnError();
        this.updateAgent = new UpdateAgent<>(this, valueProvider);
        this.updateAgent.setFilter(settings::filter);
        if (resetOnError) {
            updateAgent.setErrorHandler((uuid, valueWrapper) -> {
                if (showErrors && valueWrapper.state == ValueWrapper.State.ERROR) {
                    template.logWarning("Error on getting value for " + name + " from " + uuid + " - " + valueWrapper.errorMessage, valueWrapper.throwable);
                }
                return ValueWrapper.handled(defaultValue);
            });
        } else if (showErrors) {
            updateAgent.setErrorHandler((uuid, valueWrapper) -> {
                if (valueWrapper.state == ValueWrapper.State.ERROR) {
                    template.logWarning("Error on getting value for " + name + " from " + uuid + " - " + valueWrapper.errorMessage, valueWrapper.throwable);
                }
            });
        }
        updateAgent.setMaxSkips(template.getSettings().taskUpdateMaxSkips());
        entryAgents.add(updateAgent);
        agents.add(template.createTaskAgent(updateAgent.getUpdateRunnable(template.getSettings().taskUpdateEntryPerTick()), isAsync, template.getSettings().taskUpdateDelay()));
        agents.add(template.createTaskAgent(updateAgent.getSetRunnable(), true, template.getSettings().taskUpdateSetDelay()));

        this.snapshotAgent = SnapshotAgent.create(this);
        boolean reverseOrder = settings.reverse();
        snapshotAgent.setComparator(reverseOrder ? Comparator.naturalOrder() : Comparator.reverseOrder());
        snapshotAgent.setFilter(entry -> entry.getValue() != null);
        agents.add(snapshotAgent);
        agents.add(template.createTaskAgent(snapshotAgent, true, 20L));

        entryAgents.add(new DataEntryAgent<UUID, Double>() {
            @Override
            public void onUpdate(DataEntry<UUID, Double> entry, Double oldValue, Double newValue) {
                template.getEntryConsumeManager().consume(new EntryConsumeManager.Context(
                        GROUP,
                        name,
                        entry.getKey(),
                        oldValue,
                        newValue
                ));
            }
        });

        template.modifyAgents(this, agents, entryAgents);
        this.agents = Collections.unmodifiableList(agents);
        this.entryAgents = Collections.unmodifiableList(entryAgents);
    }

    @Override
    public @Nullable Double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public List<Agent> getAgents() {
        return agents;
    }

    @Override
    public List<DataEntryAgent<UUID, Double>> getEntryAgents() {
        return entryAgents;
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

    public interface Settings {
        Double defaultValue();

        ValueDisplay.Settings displaySettings();

        boolean async();

        boolean showErrors();

        boolean resetOnError();

        boolean reverse();

        UpdateAgent.FilterResult filter(UUID uuid);

        Map<String, Object> valueProvider();
    }
}
