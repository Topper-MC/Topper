package me.hsgamer.topper.query.display.number;

import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class NumberDisplay<K, V extends Number> implements SimpleQueryDisplay<K, V> {
    private static final Method FORMAT_DURATION_METHOD;
    private static final Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");
    private static final String FORMAT_QUERY_DECIMAL_FORMAT_PREFIX = "decimal:";
    private static final String FORMAT_QUERY_TIME_FORMAT_PREFIX = "time:";

    static {
        Class<?> clazz;
        try {
            clazz = Class.forName("org.apache.commons.lang3.time.DurationFormatUtils");
        } catch (ClassNotFoundException e) {
            try {
                clazz = Class.forName("org.apache.commons.lang.time.DurationFormatUtils");
            } catch (ClassNotFoundException ex) {
                clazz = null;
            }
        }

        Method method = null;
        if (clazz != null) {
            try {
                method = clazz.getMethod("formatDuration", long.class, String.class);
            } catch (NoSuchMethodException e) {
                // Method not found, will return null
            }
        }

        FORMAT_DURATION_METHOD = method;
    }

    private final String line;
    private final String displayNullValue;

    protected NumberDisplay(String line, String displayNullValue) {
        this.line = line;
        this.displayNullValue = displayNullValue;
    }

    private static Map<String, String> getSettings(String query) {
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }

        final String separator = "&";
        final String keyValueSeparator = "=";
        return Arrays.stream(query.split(Pattern.quote(separator)))
                .map(s -> s.split(Pattern.quote(keyValueSeparator), 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1], (a, b) -> a));
    }

    @Override
    public @NotNull String getDisplayValue(@Nullable V value, @Nullable String formatQuery) {
        if (value == null) {
            return displayNullValue;
        }

        if (formatQuery == null) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            return decimalFormat.format(value);
        }

        if (formatQuery.equals("raw")) {
            return String.valueOf(value);
        }

        if (formatQuery.startsWith(FORMAT_QUERY_DECIMAL_FORMAT_PREFIX)) {
            Map<String, String> settings = getSettings(formatQuery.substring(FORMAT_QUERY_DECIMAL_FORMAT_PREFIX.length()));

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setRoundingMode(RoundingMode.HALF_EVEN);

            Optional.ofNullable(settings.get("decimalSeparator"))
                    .map(s -> s.charAt(0))
                    .ifPresent(symbols::setDecimalSeparator);
            Optional.ofNullable(settings.get("groupingSeparator"))
                    .map(s -> s.charAt(0))
                    .ifPresent(c -> {
                        symbols.setGroupingSeparator(c);
                        decimalFormat.setGroupingUsed(true);
                    });
            Optional.ofNullable(settings.get("groupingSize"))
                    .flatMap(s -> {
                        try {
                            return Optional.of(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    })
                    .map(Number::intValue)
                    .ifPresent(decimalFormat::setGroupingSize);
            Optional.ofNullable(settings.get("maximumFractionDigits"))
                    .flatMap(s -> {
                        try {
                            return Optional.of(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    })
                    .map(Number::intValue)
                    .ifPresent(decimalFormat::setMaximumFractionDigits);

            decimalFormat.setDecimalFormatSymbols(symbols);
            return decimalFormat.format(value);
        }

        if (formatQuery.startsWith(FORMAT_QUERY_TIME_FORMAT_PREFIX)) {
            Map<String, String> settings = getSettings(formatQuery.substring(FORMAT_QUERY_TIME_FORMAT_PREFIX.length()));

            String pattern = Optional.ofNullable(settings.get("pattern")).orElse("HH:mm:ss");
            String type = Optional.ofNullable(settings.get("type")).orElse("duration");
            long time = value.longValue();
            String unitString = Optional.ofNullable(settings.get("unit")).orElse("seconds");
            TimeUnit unit;
            if (unitString.equalsIgnoreCase("ticks")) {
                unit = TimeUnit.MILLISECONDS;
                time *= 50;
            } else {
                try {
                    unit = TimeUnit.valueOf(unitString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return "INVALID_UNIT";
                }
            }
            long millis = unit.toMillis(time);

            if (type.equalsIgnoreCase("time")) {
                DateTimeFormatter formatter;
                try {
                    formatter = DateTimeFormatter.ofPattern(pattern);
                } catch (IllegalArgumentException e) {
                    return "INVALID_FORMAT";
                }
                Instant date = Instant.ofEpochMilli(millis);
                try {
                    return formatter.format(date);
                } catch (IllegalArgumentException e) {
                    return "CANNOT_FORMAT";
                }
            } else if (type.equalsIgnoreCase("duration")) {
                if (FORMAT_DURATION_METHOD == null) {
                    return "UNSUPPORTED_DURATION_FORMAT";
                }
                try {
                    return (String) FORMAT_DURATION_METHOD.invoke(null, millis, pattern);
                } catch (Exception e) {
                    return "CANNOT_FORMAT";
                }
            }
        }

        try {
            DecimalFormat decimalFormat = new DecimalFormat(formatQuery);
            return decimalFormat.format(value);
        } catch (IllegalArgumentException e) {
            return "INVALID_FORMAT";
        }
    }

    public String getDisplayLine(int index /* 1-based */, @Nullable Map.Entry<K, V> entry) {
        String line = this.line
                .replace("{index}", String.valueOf(index))
                .replace("{uuid}", getDisplayKey(entry == null ? null : entry.getKey()))
                .replace("{name}", getDisplayName(entry == null ? null : entry.getKey()));

        V value = entry == null ? null : entry.getValue();
        Matcher matcher = VALUE_PLACEHOLDER_PATTERN.matcher(line);
        while (matcher.find()) {
            String formatType = matcher.group(1);
            line = line.replace(matcher.group(), getDisplayValue(value, formatType));
        }

        return line;
    }
}
