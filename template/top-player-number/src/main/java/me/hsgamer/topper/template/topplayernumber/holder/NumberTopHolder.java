package me.hsgamer.topper.template.topplayernumber.holder;

import me.hsgamer.topper.agent.core.AgentHolder;
import me.hsgamer.topper.agent.core.EntryEvent;
import me.hsgamer.topper.agent.core.HolderEvent;
import me.hsgamer.topper.agent.core.Notifier;
import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.agent.snapshot.SnapshotHolderAgent;
import me.hsgamer.topper.agent.storage.StorageAgent;
import me.hsgamer.topper.agent.update.UpdateAgent;
import me.hsgamer.topper.data.simple.SimpleDataHolder;
import me.hsgamer.topper.query.display.number.NumberDisplay;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.manager.EntryConsumeManager;
import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NumberTopHolder extends SimpleDataHolder<UUID, Double> implements AgentHolder<UUID, Double> {
    public static final String GROUP = "topper";

    private final String name;
    private final Settings settings;
    private final NumberDisplay<UUID, Double> valueDisplay;
    private final Notifier<EntryEvent<UUID, Double>> entryNotifier = new Notifier<>();
    private final Notifier<HolderEvent> holderNotifier = new Notifier<>();
    private final StorageAgent<UUID, Double> storageAgent;
    private final UpdateAgent<UUID, Double> updateAgent;
    private final SnapshotHolderAgent<UUID, Double> snapshotAgent;
    private final Double defaultValue;

    public NumberTopHolder(TopPlayerNumberTemplate template, String name, Settings settings) {
        this.name = name;
        this.settings = settings;

        ValueWrapper<Double> defaultValueWrapper = settings.defaultValue();
        switch (defaultValueWrapper.state) {
            case HANDLED:
                this.defaultValue = defaultValueWrapper.value;
                break;
            case ERROR:
                template.logWarning("Error when parsing default value in " + name + " - " + defaultValueWrapper.errorMessage, defaultValueWrapper.throwable);
                this.defaultValue = null;
                break;
            default:
                this.defaultValue = null;
                break;
        }

        this.valueDisplay = new NumberDisplay<UUID, Double>() {
            @Override
            public @NotNull String getDisplayName(@Nullable UUID uuid) {
                return Optional.ofNullable(uuid).map(template.getNameProviderManager()::getName).orElse(settings.displayNullName());
            }

            @Override
            public @NotNull String getDisplayKey(@Nullable UUID uuid) {
                return uuid != null ? uuid.toString() : settings.displayNullUuid();
            }

            @Override
            public @NotNull String getDisplayNullValue() {
                return settings.displayNullValue();
            }

            @Override
            public @NotNull String getDisplayValue(@Nullable Double value, @NotNull String formatQuery) {
                if (formatQuery.isEmpty()) {
                    formatQuery = settings.defaultValueDisplay();
                }
                return super.getDisplayValue(value, formatQuery);
            }
        };

        this.storageAgent = new StorageAgent<>(template.getTopManager().buildStorage(name));
        storageAgent.setMaxEntryPerCall(template.getSettings().taskSaveEntryPerTick());
        storageAgent.bindTo(this);
        storageAgent.bindLoadTo(this);
        bindAutoTask(template, storageAgent, TaskType.STORAGE, settings.valueProvider());

        ValueProvider<UUID, Double> valueProvider = template.createValueProvider(settings.valueProvider()).orElseGet(() -> {
            template.logWarning("No value provider found for " + name);
            return ValueProvider.empty();
        });
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
        updateAgent.bindTo(this);
        bindAutoTask(template, updateAgent.getUpdateRunnable(template.getSettings().taskUpdateEntryPerTick()), TaskType.UPDATE, settings.valueProvider());
        bindAutoTask(template, updateAgent.getSetRunnable(), TaskType.SET, settings.valueProvider());

        this.snapshotAgent = new SnapshotHolderAgent<>(this);
        boolean reverseOrder = settings.reverse();
        snapshotAgent.setComparator(reverseOrder ? Comparator.naturalOrder() : Comparator.reverseOrder());
        snapshotAgent.setDataFilter(entry -> entry.getValue() != null);
        snapshotAgent.bindTo(this);
        bindAutoTask(template, snapshotAgent, TaskType.SNAPSHOT, settings.valueProvider());

        getEntryNotifier().addListener(e -> {
            if (e.kind != EntryEvent.Kind.UPDATED) {
                return;
            }
            template.getEntryConsumeManager().consume(new EntryConsumeManager.Context(
                    GROUP,
                    name,
                    e.entry.getKey(),
                    e.oldValue,
                    e.newValue
            ));
        });
        template.modifyNotifiers(this);
    }

    private void bindAutoTask(TopPlayerNumberTemplate template, Runnable runnable, TaskType taskType, Map<String, Object> settings) {
        Runnable unregister = template.bindTask(this, runnable, taskType, settings);
        getHolderNotifier().addListener(e -> {
            if (e != HolderEvent.UNREGISTERED) {
                return;
            }
            unregister.run();
        });
    }

    @Override
    public @Nullable Double getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Notifier<EntryEvent<UUID, Double>> getEntryNotifier() {
        return entryNotifier;
    }

    @Override
    public Notifier<HolderEvent> getHolderNotifier() {
        return holderNotifier;
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

    public NumberDisplay<UUID, Double> getValueDisplay() {
        return valueDisplay;
    }

    public String getName() {
        return name;
    }

    public Settings getSettings() {
        return settings;
    }

    public enum TaskType {
        STORAGE,
        SET,
        SNAPSHOT,
        UPDATE
    }

    public interface Settings {
        ValueWrapper<Double> defaultValue();

        String displayNullName();

        String displayNullUuid();

        String displayNullValue();

        default String defaultValueDisplay() {
            return "";
        }

        boolean showErrors();

        boolean resetOnError();

        boolean reverse();

        UpdateAgent.FilterResult filter(UUID uuid);

        Map<String, Object> valueProvider();
    }

    public static abstract class MapSettings implements Settings {
        protected final Map<String, Object> map;

        protected MapSettings(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public ValueWrapper<Double> defaultValue() {
            Object value = map.get("default-value");
            if (value == null) {
                return ValueWrapper.notHandled();
            }

            try {
                double numberValue = Double.parseDouble(Objects.toString(value));
                return ValueWrapper.handled(numberValue);
            } catch (Exception e) {
                return ValueWrapper.error("Invalid number: \"" + value + "\". Fallback to null", e);
            }
        }

        @Override
        public String defaultValueDisplay() {
            return Optional.ofNullable(map.get("default-value-display"))
                    .map(Object::toString)
                    .orElse(Settings.super.defaultValueDisplay());
        }

        @Override
        public String displayNullName() {
            return Optional.ofNullable(map.get("null-name"))
                    .map(Object::toString)
                    .orElse("---");
        }

        @Override
        public String displayNullUuid() {
            return Optional.ofNullable(map.get("null-uuid"))
                    .map(Object::toString)
                    .orElse("---");
        }

        @Override
        public String displayNullValue() {
            return Optional.ofNullable(map.get("null-value"))
                    .map(Object::toString)
                    .orElse("---");
        }

        @Override
        public boolean showErrors() {
            return Optional.ofNullable(map.get("show-errors"))
                    .map(Object::toString)
                    .map(String::toLowerCase)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        }

        @Override
        public boolean resetOnError() {
            return Optional.ofNullable(map.get("reset-on-error"))
                    .map(Object::toString)
                    .map(String::toLowerCase)
                    .map(Boolean::parseBoolean)
                    .orElse(true);
        }

        @Override
        public boolean reverse() {
            return Optional.ofNullable(map.get("reverse"))
                    .map(String::valueOf)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
        }

        @Override
        public Map<String, Object> valueProvider() {
            return map;
        }

        public Map<String, Object> map() {
            return map;
        }
    }
}
