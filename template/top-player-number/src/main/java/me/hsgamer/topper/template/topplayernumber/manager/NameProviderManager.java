package me.hsgamer.topper.template.topplayernumber.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class NameProviderManager {
    private final List<Function<UUID, String>> nameProviders = new ArrayList<>();
    private Function<UUID, String> defaultNameProvider = null;

    public Runnable addNameProvider(Function<UUID, String> nameProvider) {
        nameProviders.add(nameProvider);
        return () -> nameProviders.remove(nameProvider);
    }

    public void setDefaultNameProvider(Function<UUID, String> defaultNameProvider) {
        this.defaultNameProvider = defaultNameProvider;
    }

    public String getName(UUID uuid) {
        for (Function<UUID, String> nameProvider : nameProviders) {
            String name = nameProvider.apply(uuid);
            if (name != null) {
                return name;
            }
        }
        if (defaultNameProvider != null) {
            return defaultNameProvider.apply(uuid);
        }
        return null;
    }

    public void clear() {
        nameProviders.clear();
        defaultNameProvider = null;
    }
}
