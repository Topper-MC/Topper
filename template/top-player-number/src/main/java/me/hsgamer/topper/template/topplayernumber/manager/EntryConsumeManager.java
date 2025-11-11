package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class EntryConsumeManager {
    private final TopPlayerNumberTemplate template;
    private final List<Consumer<Context>> consumerList = new ArrayList<>();
    private final Map<String, Provider> providerMap = new HashMap<>();

    public EntryConsumeManager(TopPlayerNumberTemplate template) {
        this.template = template;
    }

    public Runnable addConsumer(Consumer<Context> consumer) {
        consumerList.add(consumer);
        return () -> consumerList.remove(consumer);
    }

    public Runnable addConsumer(String group, String holder, BiConsumer<UUID, Double> consumer) {
        return addConsumer((context) -> {
            if (Objects.equals(context.group, group) && Objects.equals(context.holder, holder)) {
                consumer.accept(context.uuid, context.value);
            }
        });
    }

    public Runnable addProvider(String group, Provider provider) {
        providerMap.put(group, provider);
        return () -> providerMap.remove(group);
    }

    public Provider getProvider(String group) {
        return providerMap.getOrDefault(group, Provider.DEFAULT);
    }

    public Map<String, Provider> getProviderMap() {
        return Collections.unmodifiableMap(providerMap);
    }

    public void consume(Context context) {
        consumerList.forEach(consumer -> consumer.accept(context));
    }

    public void enable() {
        addProvider(NumberTopHolder.GROUP, new Provider() {
            @Override
            public Collection<String> getHolders() {
                return template.getTopManager().getHolderNames();
            }

            @Override
            public Optional<Double> getValue(String holder, UUID uuid) {
                return template.getTopManager()
                        .getHolder(holder)
                        .flatMap(h -> h.getEntry(uuid))
                        .map(DataEntry::getValue);
            }

            @Override
            public Optional<String> getName(String holder, UUID uuid) {
                return Optional.ofNullable(template.getNameProviderManager().getName(uuid));
            }

            @Override
            public Optional<Integer> getSnapshotIndex(String holder, UUID uuid) {
                return template.getTopManager()
                        .getHolder(holder)
                        .map(NumberTopHolder::getSnapshotAgent)
                        .map(agent -> agent.getSnapshotIndex(uuid));
            }
        });
    }

    public void disable() {
        consumerList.clear();
        providerMap.clear();
    }

    public interface Provider {
        Provider DEFAULT = new Provider() {
        };

        default Collection<String> getHolders() {
            return Collections.emptyList();
        }

        default Optional<Double> getValue(String holder, UUID uuid) {
            return Optional.empty();
        }

        default Optional<String> getName(String holder, UUID uuid) {
            return Optional.empty();
        }

        default Optional<Integer> getSnapshotIndex(String holder, UUID uuid) {
            return Optional.empty();
        }
    }

    public static class Context {
        public final String group;
        public final String holder;
        public final UUID uuid;
        public final @Nullable Double oldValue;
        public final @Nullable Double value;

        public Context(String group, String holder, UUID uuid, @Nullable Double oldValue, @Nullable Double value) {
            this.group = group;
            this.holder = holder;
            this.uuid = uuid;
            this.oldValue = oldValue;
            this.value = value;
        }
    }
}
