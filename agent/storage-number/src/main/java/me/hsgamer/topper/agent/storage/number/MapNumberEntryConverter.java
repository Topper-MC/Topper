package me.hsgamer.topper.agent.storage.number;

import me.hsgamer.topper.agent.storage.simple.converter.MapEntryConverter;

import java.util.Collections;
import java.util.Map;

public interface MapNumberEntryConverter<K> extends MapEntryConverter<K, Double> {
    @Override
    default Double toValue(Map<String, Object> map) {
        Object object = map.get("value");
        if (object instanceof Number) {
            return ((Number) object).doubleValue();
        } else {
            try {
                return Double.parseDouble(object.toString());
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Override
    default Map<String, Object> toRawValue(Double object) {
        return Collections.singletonMap("value", object);
    }
}
