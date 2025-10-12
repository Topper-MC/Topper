package me.hsgamer.topper.template.topplayernumber.manager;

import me.hsgamer.topper.template.topplayernumber.TopPlayerNumberTemplate;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;

import java.util.*;

public class TopManager {
    private final Map<String, NumberTopHolder> holders = new HashMap<>();
    private final TopPlayerNumberTemplate template;

    public TopManager(TopPlayerNumberTemplate template) {
        this.template = template;
    }

    public void enable() {
        template.getSettings().holders().forEach((key, value) -> {
            NumberTopHolder topHolder = new NumberTopHolder(template, key, value);
            topHolder.register();
            holders.put(key, topHolder);
        });
    }

    public void disable() {
        holders.values().forEach(NumberTopHolder::unregister);
        holders.clear();
    }

    public Optional<NumberTopHolder> getHolder(String name) {
        return Optional.ofNullable(holders.get(name));
    }

    public List<String> getHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(holders.keySet()));
    }

    public void create(UUID uuid) {
        holders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}
