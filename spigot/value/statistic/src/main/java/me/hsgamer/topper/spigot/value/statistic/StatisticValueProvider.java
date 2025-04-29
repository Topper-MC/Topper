package me.hsgamer.topper.spigot.value.statistic;

import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StatisticValueProvider implements ValueProvider<Player, Integer> {
    private final Function<Player, ValueWrapper<Integer>> getFunction;

    private StatisticValueProvider(@NotNull Function<Player, ValueWrapper<Integer>> getFunction) {
        this.getFunction = getFunction;
    }

    private static <T> StatisticValueProvider collection(Collection<T> collection, BiFunction<Player, T, Integer> mapper) {
        return new StatisticValueProvider(player -> {
            int total = 0;
            for (T entry : collection) {
                if (entry == null) {
                    return ValueWrapper.error("Null values provided");
                }

                try {
                    Integer value = mapper.apply(player, entry);
                    if (value == null) {
                        return ValueWrapper.error("The raw value is null");
                    }
                    total += value;
                } catch (Throwable e) {
                    return ValueWrapper.error("An error occurred while getting the value for " + entry, e);
                }
            }
            return ValueWrapper.handled(total);
        });
    }

    private static <T> StatisticValueProvider cachedCollection(Supplier<Stream<T>> supplier, BiFunction<Player, T, Integer> mapper) {
        return new StatisticValueProvider(new Function<Player, ValueWrapper<Integer>>() {
            private final AtomicReference<Collection<T>> cachedEntriesRef = new AtomicReference<>();

            private Collection<T> getCachedEntries(Player player) {
                Collection<T> cachedEntries = cachedEntriesRef.get();
                if (cachedEntries == null) {
                    cachedEntries = supplier.get()
                            .filter(Objects::nonNull)
                            .filter(entry -> {
                                try {
                                    return mapper.apply(player, entry) != null;
                                } catch (Throwable e) {
                                    return false;
                                }
                            })
                            .collect(Collectors.toList());
                    cachedEntriesRef.set(cachedEntries);
                }
                return cachedEntries;
            }

            @Override
            public ValueWrapper<Integer> apply(Player player) {
                int total = getCachedEntries(player)
                        .stream()
                        .filter(Objects::nonNull)
                        .map(entry -> {
                            try {
                                return mapper.apply(player, entry);
                            } catch (Throwable e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .reduce(0, Integer::sum);
                return ValueWrapper.handled(total);
            }
        });
    }

    public static StatisticValueProvider of(Statistic statistic) {
        return new StatisticValueProvider(player -> {
            try {
                return ValueWrapper.handled(player.getStatistic(statistic));
            } catch (Throwable e) {
                return ValueWrapper.error("An error occurred while getting the value for " + statistic, e);
            }
        });
    }

    public static StatisticValueProvider ofMaterial(Statistic statistic, Collection<Material> materials) {
        return collection(materials, (player, material) -> player.getStatistic(statistic, material));
    }

    public static StatisticValueProvider ofEntity(Statistic statistic, Collection<EntityType> entityTypes) {
        return collection(entityTypes, (player, entity) -> player.getStatistic(statistic, entity));
    }

    public static StatisticValueProvider ofMaterial(Statistic statistic) {
        return cachedCollection(
                () -> Arrays.stream(Material.values()).filter(material -> !material.name().startsWith("LEGACY_")),
                (player, material) -> player.getStatistic(statistic, material)
        );
    }

    public static StatisticValueProvider ofEntity(Statistic statistic) {
        return cachedCollection(
                () -> Arrays.stream(EntityType.values()),
                (player, entity) -> player.getStatistic(statistic, entity)
        );
    }

    public static ValueProvider<Player, Integer> fromRaw(String statistic, Collection<String> materials, Collection<String> entityTypes) {
        Statistic stat = Optional.ofNullable(statistic)
                .map(String::toUpperCase)
                .flatMap(s -> {
                    try {
                        return Optional.of(Statistic.valueOf(s));
                    } catch (IllegalArgumentException e) {
                        return Optional.empty();
                    }
                })
                .orElse(null);
        if (stat == null) {
            return ValueProvider.error("Invalid statistic: " + statistic);
        }

        switch (stat.getType()) {
            case BLOCK:
            case ITEM:
                if (materials.isEmpty()) {
                    return ofMaterial(stat);
                }
                return ofMaterial(stat, materials.stream()
                        .map(Material::matchMaterial)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            case ENTITY:
                if (entityTypes.isEmpty()) {
                    return ofEntity(stat);
                }
                return ofEntity(stat, entityTypes.stream()
                        .map(String::toUpperCase)
                        .flatMap(s -> {
                            try {
                                return Stream.of(EntityType.valueOf(s));
                            } catch (IllegalArgumentException e) {
                                return Stream.empty();
                            }
                        })
                        .collect(Collectors.toList()));
            default:
                return of(stat);
        }
    }

    @Override
    public @NotNull ValueWrapper<Integer> apply(@NotNull Player player) {
        return getFunction.apply(player);
    }
}
