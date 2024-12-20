package me.hsgamer.topper.storage.simple.converter;

import java.util.Map;

public interface MapEntryConverter<K, V> {
    K toKey(Map<String, Object> map);

    V toValue(Map<String, Object> map);

    Map<String, Object> toRawKey(K key);

    Map<String, Object> toRawValue(V value);
}
