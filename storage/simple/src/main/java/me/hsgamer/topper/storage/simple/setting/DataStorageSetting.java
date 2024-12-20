package me.hsgamer.topper.storage.simple.setting;

import me.hsgamer.topper.storage.simple.converter.FlatEntryConverter;
import me.hsgamer.topper.storage.simple.converter.MapEntryConverter;
import me.hsgamer.topper.storage.simple.converter.SqlEntryConverter;

public interface DataStorageSetting<K, V> {
    FlatEntryConverter<K, V> getFlatEntryConverter();

    MapEntryConverter<K, V> getMapEntryConverter();

    SqlEntryConverter<K, V> getSqlEntryConverter();
}
