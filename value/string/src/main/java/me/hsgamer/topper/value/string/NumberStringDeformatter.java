package me.hsgamer.topper.value.string;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class NumberStringDeformatter implements UnaryOperator<String> {
    private final Settings settings;

    public NumberStringDeformatter(Settings settings) {
        this.settings = settings;
    }

    public static UnaryOperator<String> deformatterOrIdentity(Map<String, Object> map) {
        boolean needDeformat = Optional.ofNullable(map.get("formatted"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
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
        return needDeformat ? new NumberStringDeformatter(Settings.fromMap(deformatterMap)) : UnaryOperator.identity();
    }

    @Override
    public String apply(String string) {
        StringBuilder builder = new StringBuilder();
        boolean decimalSeparatorFound = false;
        for (char c : string.toCharArray()) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else if (!decimalSeparatorFound && c == settings.decimalSeparator) {
                builder.append('.');
                decimalSeparatorFound = true;
            }
        }
        return builder.toString().trim();
    }

    public static class Settings {
        public char decimalSeparator = '.';

        public static Settings fromMap(Map<String, Object> map) {
            Settings settings = new Settings();
            settings.decimalSeparator = Optional.ofNullable(map.get("decimal-separator"))
                    .map(Object::toString)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.charAt(0))
                    .orElse('.');
            return settings;
        }
    }
}
