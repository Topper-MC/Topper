package me.hsgamer.topper.agent.storage.simple.setting;

import me.hsgamer.topper.agent.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.agent.storage.simple.converter.MapEntryConverter;
import me.hsgamer.topper.agent.storage.simple.converter.SqlEntryConverter;

import java.io.File;

public interface DataStorageBuilderSetting<K, V> {
    DatabaseSetting getDatabaseSetting();

    File getBaseFolder();

    FlatEntryConverter<K, V> getFlatEntryConverter();

    MapEntryConverter<K, V> getMapEntryConverter();

    SqlEntryConverter<K, V> getSqlEntryConverter();
}
