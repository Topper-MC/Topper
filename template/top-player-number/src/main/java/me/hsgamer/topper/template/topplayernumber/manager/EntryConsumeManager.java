package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.data.core.DataEntry;
import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class EntryConsumeManager {
    private final TopPlayerNumberTemplate template;
    private final List<Consumer<Context>> consumerList = new ArrayList<>();
    private final Map<String, BiFunction<String, UUID, Optional<Double>>> providerMap = new HashMap<>();

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

    public Runnable addProvider(String group, BiFunction<String, UUID, Optional<Double>> provider) {
        providerMap.put(group, provider);
        return () -> providerMap.remove(group);
    }

    public Optional<Double> getValue(String group, String holder, UUID uuid) {
        BiFunction<String, UUID, Optional<Double>> function = providerMap.get(group);
        return function == null ? Optional.empty() : function.apply(holder, uuid);
    }

    public void consume(Context context) {
        consumerList.forEach(consumer -> consumer.accept(context));
    }

    public void enable() {
        addProvider(NumberTopHolder.GROUP, (holder, uuid) ->
                template.getTopManager()
                        .getHolder(holder)
                        .flatMap(h -> h.getEntry(uuid))
                        .map(DataEntry::getValue)
        );
    }

    public void disable() {
        consumerList.clear();
        providerMap.clear();
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
