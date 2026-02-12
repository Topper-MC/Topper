package me.hsgamer.topper.query.display.number;

import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.value.timeformat.DateTimeFormatters;
import me.hsgamer.topper.value.timeformat.DurationTimeFormatters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class NumberDisplay<K, V extends Number> implements SimpleQueryDisplay<K, V> {
    private static final String FORMAT_QUERY_DECIMAL = "decimal";
    private static final String FORMAT_QUERY_TIME = "time";
    private static final String FORMAT_QUERY_SHORTEN = "shorten";
    private static final Map<String, Function<Number, String>> displayByQueryCache = new ConcurrentHashMap<>();

    private static Map<String, String> getSettings(String query) {
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }

        // Handle prefix-setting separator if exists
        if (query.startsWith(":")) {
            query = query.substring(1);
        }

        final String separator = "&";
        final String keyValueSeparator = "=";
        return Arrays.stream(query.split(Pattern.quote(separator)))
                .map(s -> s.split(Pattern.quote(keyValueSeparator), 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1], (a, b) -> a));
    }

    private static Function<Number, String> createDisplayFunction(String formatQuery) {
        if (formatQuery.startsWith(FORMAT_QUERY_SHORTEN)) {
            String config = formatQuery.substring(FORMAT_QUERY_SHORTEN.length());
            NavigableMap<Double, String> suffixMap = new TreeMap<>();

            if (config.isEmpty()) {
                suffixMap.put(1000.0, "k");
                suffixMap.put(1000000.0, "M");
                suffixMap.put(1000000000.0, "B");
                suffixMap.put(1000000000000.0, "T");
            } else {
                Map<String, String> settings = getSettings(config);
                for (Map.Entry<String, String> entry : settings.entrySet()) {
                    try {
                        double threshold = Double.parseDouble(entry.getKey());
                        String suffix = entry.getValue();
                        suffixMap.put(threshold, suffix);
                    } catch (NumberFormatException ignored) {
                        return n -> "INVALID_NUMBER_FOR_SHORTEN_SUFFIX";
                    }
                }
            }
            return n -> {
                double value = n.doubleValue();
                if (suffixMap.isEmpty()) {
                    if (value == (long) value) {
                        return String.format(Locale.ENGLISH, "%,d", (long) value);
                    } else {
                        BigDecimal bd = BigDecimal.valueOf(value);
                        bd = bd.setScale(2, RoundingMode.DOWN);
                        return bd.stripTrailingZeros().toPlainString();
                    }
                }

                boolean isNegative = value < 0;
                double absValue = Math.abs(value);

                Map.Entry<Double, String> entry = suffixMap.floorEntry(absValue);
                if (entry == null || entry.getKey() == 0 || absValue < 1000) {
                    if (absValue == (long) absValue) {
                        return (isNegative ? "-" : "") + String.format(Locale.ENGLISH, "%,d", (long) absValue);
                    } else {
                        BigDecimal bd = BigDecimal.valueOf(absValue);
                        bd = bd.setScale(2, RoundingMode.DOWN);
                        String formatted = bd.stripTrailingZeros().toPlainString();
                        return (isNegative ? "-" : "") + formatted;
                    }
                }

                double threshold = entry.getKey();
                String suffix = entry.getValue();
                BigDecimal divided = BigDecimal.valueOf(absValue)
                        .divide(BigDecimal.valueOf(threshold), 10, RoundingMode.DOWN);

                divided = divided.setScale(2, RoundingMode.DOWN);
                String formatted = divided.stripTrailingZeros().toPlainString();

                return (isNegative ? "-" : "") + formatted + suffix;
            };
        }

        if (formatQuery.startsWith(FORMAT_QUERY_TIME)) {
            Map<String, String> settings = getSettings(formatQuery.substring(FORMAT_QUERY_TIME.length()));

            String unitString = Optional.ofNullable(settings.get("unit")).orElse("ticks");
            Function<Number, Long> toMillis;
            if (unitString.equalsIgnoreCase("ticks")) {
                toMillis = n -> n.longValue() * 50;
            } else {
                try {
                    TimeUnit unit = TimeUnit.valueOf(unitString.toUpperCase());
                    toMillis = n -> unit.toMillis(n.longValue());
                } catch (IllegalArgumentException e) {
                    return n -> "INVALID_UNIT";
                }
            }

            String type = Optional.ofNullable(settings.get("type")).orElse("duration");
            Optional<String> patternOptional = Optional.ofNullable(settings.get("pattern"));
            if (type.equalsIgnoreCase("time")) {
                String pattern = patternOptional.orElse("RFC_1123_DATE_TIME");
                Optional<DateTimeFormatter> formatterOptional = DateTimeFormatters.getFormatter(pattern);
                if (!formatterOptional.isPresent()) {
                    return n -> "INVALID_FORMAT";
                }
                DateTimeFormatter formatter = formatterOptional.get();

                try {
                    return n -> {
                        long millis = toMillis.apply(n.longValue());
                        Instant date = Instant.ofEpochMilli(millis);
                        return formatter.format(date);
                    };
                } catch (IllegalArgumentException e) {
                    return n -> "CANNOT_FORMAT";
                }
            } else if (type.equalsIgnoreCase("duration")) {
                String pattern = patternOptional.orElse("default");
                if (pattern.equalsIgnoreCase("default")) {
                    return toMillis.andThen(millis -> DurationTimeFormatters.formatDuration(millis, "HH:mm:ss"));
                } else if (pattern.equalsIgnoreCase("word")) {
                    return toMillis.andThen(millis -> DurationTimeFormatters.formatDurationWords(millis, true, true));
                } else if (pattern.equalsIgnoreCase("short")) {
                    return toMillis.andThen(millis -> DurationTimeFormatters.formatDuration(millis, "[H:]m:ss"));
                } else if (pattern.equalsIgnoreCase("short-word")) {
                    return toMillis.andThen(millis -> DurationTimeFormatters.formatDuration(millis, "[d'd 'H'h 'm'm 's's']"));
                } else {
                    return toMillis.andThen(millis -> DurationTimeFormatters.formatDuration(millis, pattern));
                }
            }
        }

        if (formatQuery.startsWith(FORMAT_QUERY_DECIMAL)) {
            Map<String, String> settings = getSettings(formatQuery.substring(FORMAT_QUERY_DECIMAL.length()));

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

            return n -> ((DecimalFormat) decimalFormat.clone()).format(n);
        }

        try {
            DecimalFormat decimalFormat = new DecimalFormat(formatQuery);
            return n -> ((DecimalFormat) decimalFormat.clone()).format(n);
        } catch (IllegalArgumentException e) {
            return n -> "INVALID_FORMAT";
        }
    }

    public abstract @NotNull String getDisplayNullValue();

    @Override
    public @NotNull String getDisplayValue(@Nullable V value, @NotNull String formatQuery) {
        if (value == null) {
            return getDisplayNullValue();
        }

        if (formatQuery.isEmpty()) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            return decimalFormat.format(value);
        }

        if (formatQuery.equals("raw")) {
            return String.valueOf(value);
        }

        return displayByQueryCache.computeIfAbsent(formatQuery, NumberDisplay::createDisplayFunction).apply(value);
    }
}