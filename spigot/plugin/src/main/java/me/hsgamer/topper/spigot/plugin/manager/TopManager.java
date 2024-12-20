package me.hsgamer.topper.spigot.plugin.manager;

import io.github.projectunified.minelib.plugin.base.Loadable;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.hscore.config.proxy.ConfigGenerator;
import me.hsgamer.hscore.database.Setting;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.config.DatabaseConfig;
import me.hsgamer.topper.spigot.plugin.config.MainConfig;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import me.hsgamer.topper.storage.core.DataStorage;
import me.hsgamer.topper.storage.number.FlatNumberEntryConverter;
import me.hsgamer.topper.storage.number.MapNumberEntryConverter;
import me.hsgamer.topper.storage.number.SqlNumberEntryConverter;
import me.hsgamer.topper.storage.simple.builder.DataStorageBuilder;
import me.hsgamer.topper.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.storage.simple.converter.MapEntryConverter;
import me.hsgamer.topper.storage.simple.converter.SqlEntryConverter;
import me.hsgamer.topper.storage.simple.setting.DataStorageBuilderSetting;
import me.hsgamer.topper.storage.simple.setting.DataStorageSetting;
import me.hsgamer.topper.storage.simple.supplier.DataStorageSupplier;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;

public class TopManager implements Loadable {
    private final Map<String, NumberTopHolder> topHolders = new HashMap<>();
    private final TopperPlugin instance;
    private DataStorageSupplier storageSupplier;

    public TopManager(TopperPlugin instance) {
        this.instance = instance;
    }

    @Override
    public void enable() {
        storageSupplier = instance.get(DataStorageBuilder.class).buildSupplier(
                instance.get(MainConfig.class).getStorageType(),
                new DataStorageBuilderSetting() {
                    @Override
                    public Consumer<Setting> getDatabaseSettingModifier() {
                        return ConfigGenerator.newInstance(DatabaseConfig.class, new BukkitConfig(instance, "database.yml"));
                    }

                    @Override
                    public File getBaseFolder() {
                        return new File(instance.getDataFolder(), "top");
                    }
                }
        );
        storageSupplier.enable();
        instance.get(MainConfig.class).getHolders().forEach((key, value) -> {
            NumberTopHolder topHolder = new NumberTopHolder(instance, key, value);
            topHolder.register();
            topHolders.put(key, topHolder);
        });
    }

    @Override
    public void disable() {
        topHolders.values().forEach(NumberTopHolder::unregister);
        topHolders.clear();
        storageSupplier.disable();
    }

    public DataStorageSupplier getStorageSupplier() {
        return storageSupplier;
    }

    public DataStorage<UUID, Double> buildStorage(String name) {
        return storageSupplier.getStorage(name, new DataStorageSetting<UUID, Double>() {
            @Override
            public FlatEntryConverter<UUID, Double> getFlatEntryConverter() {
                return new FlatNumberEntryConverter<UUID>() {
                    @Override
                    public UUID toKey(String key) {
                        return UUID.fromString(key);
                    }

                    @Override
                    public String toRawKey(UUID uuid) {
                        return uuid.toString();
                    }
                };
            }

            @Override
            public MapEntryConverter<UUID, Double> getMapEntryConverter() {
                return new MapNumberEntryConverter<UUID>() {
                    @Override
                    public UUID toKey(Map<String, Object> map) {
                        Object key = map.get("uuid");
                        if (key != null) {
                            try {
                                return UUID.fromString(key.toString());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public Map<String, Object> toRawKey(UUID key) {
                        return Collections.singletonMap("uuid", key.toString());
                    }
                };
            }

            @Override
            public SqlEntryConverter<UUID, Double> getSqlEntryConverter() {
                return new SqlNumberEntryConverter<UUID>() {
                    @Override
                    public String[] getKeyColumns() {
                        return new String[]{"uuid"};
                    }

                    @Override
                    public UUID getKey(ResultSet resultSet) throws SQLException {
                        return UUID.fromString(resultSet.getString("uuid"));
                    }

                    @Override
                    public String[] getKeyColumnDefinitions() {
                        return new String[]{"`uuid` varchar(36) NOT NULL"};
                    }

                    @Override
                    public Object[] toKeyQueryValues(UUID key) {
                        return new Object[]{key.toString()};
                    }
                };
            }
        });
    }

    public Optional<NumberTopHolder> getTopHolder(String name) {
        return Optional.ofNullable(topHolders.get(name));
    }

    public List<String> getTopHolderNames() {
        return Collections.unmodifiableList(new ArrayList<>(topHolders.keySet()));
    }

    public void create(UUID uuid) {
        topHolders.values().forEach(holder -> holder.getOrCreateEntry(uuid));
    }
}
