package me.hsgamer.topper.value.timeformat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class DateTimeFormatters {
    private static final Map<String, DateTimeFormatter> FORMATTER_MAP = new ConcurrentHashMap<>();

    private DateTimeFormatters() {
        // Prevent instantiation
    }

    public static Optional<DateTimeFormatter> getFormatter(String name) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        DateTimeFormatter formatter = FORMATTER_MAP.get(name);
        if (formatter != null) {
            return Optional.of(formatter);
        }

        Class<DateTimeFormatter> dateTimeFormatterClass = DateTimeFormatter.class;
        Field field;
        try {
            field = dateTimeFormatterClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            field = null;
        }
        if (field != null && Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == DateTimeFormatter.class) {
            try {
                formatter = (DateTimeFormatter) field.get(null);
                FORMATTER_MAP.put(name, formatter);
                return Optional.of(formatter);
            } catch (IllegalAccessException e) {
                // Ignore and try to create a new formatter
            }
        }

        try {
            formatter = DateTimeFormatter.ofPattern(name);
            FORMATTER_MAP.put(name, formatter);
            return Optional.of(formatter);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
