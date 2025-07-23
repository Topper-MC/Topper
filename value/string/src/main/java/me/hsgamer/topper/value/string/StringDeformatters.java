package me.hsgamer.topper.value.string;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class StringDeformatters {
    public static NumberStringDeformatter numberStringDeformatter(Map<String, Object> map) {
        char decimalSeparator = Optional.ofNullable(map.get("decimal-separator"))
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .map(s -> s.charAt(0))
                .orElse('.');
        return new NumberStringDeformatter(decimalSeparator);
    }

    public static TimeStringDeformatter timeStringDeformatter(Map<String, Object> map) {
        String timeFormat = Optional.ofNullable(map.get("time-format"))
                .map(Object::toString)
                .orElse("HH:mm:ss");
        return new TimeStringDeformatter(timeFormat);
    }

    public static UnaryOperator<String> deformatterOrIdentity(Map<String, Object> map) {
        boolean needDeformat = Optional.ofNullable(map.get("formatted"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        if (!needDeformat) {
            return UnaryOperator.identity();
        }

        Map<String, Object> deformatterMap = Optional.ofNullable(map.get("formatted-settings"))
                .flatMap(rawMap -> {
                    if (rawMap instanceof Map) {
                        Map<String, Object> castedMap = new HashMap<>();
                        ((Map<?, ?>) rawMap).forEach((key, value) -> castedMap.put(key.toString(), value));
                        return Optional.of(castedMap);
                    } else {
                        return Optional.empty();
                    }
                })
                .orElseGet(Collections::emptyMap);
        Type type = Optional.ofNullable(map.get("formatted-type"))
                .map(Object::toString)
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return Type.valueOf(s);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(Type.NUMBER);
        switch (type) {
            case NUMBER:
                return numberStringDeformatter(deformatterMap);
            case TIME:
                return timeStringDeformatter(deformatterMap);
            default:
                return UnaryOperator.identity();
        }
    }

    private enum Type {
        NUMBER,
        TIME,
    }
}
