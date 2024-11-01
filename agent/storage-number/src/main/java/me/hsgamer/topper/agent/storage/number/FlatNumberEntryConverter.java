package me.hsgamer.topper.agent.storage.number;

import me.hsgamer.topper.agent.storage.simple.converter.FlatEntryConverter;

public interface FlatNumberEntryConverter<K> extends FlatEntryConverter<K, Double> {
    @Override
    default Double toValue(String object) {
        try {
            return Double.parseDouble(object);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    default String toRawValue(Double object) {
        return Double.toString(object);
    }
}
