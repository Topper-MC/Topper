package me.hsgamer.topper.query.display.number;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.value.timeformat.DateTimeFormatters;
import me.hsgamer.topper.value.timeformat.DurationTimeFormatters;

public abstract class NumberDisplay<K, V extends Number> implements SimpleQueryDisplay<K, V> {
    private static final Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");
    private static final String FORMAT_QUERY_DECIMAL_FORMAT_PREFIX = "decimal:";
    private static final String FORMAT_QUERY_TIME_FORMAT_PREFIX = "time:";

    private final String line;
    private final String displayNullValue;
    private final NavigableMap<Double, String> customFormats;

    protected NumberDisplay(String line, String displayNullValue) {
        this(line, displayNullValue, new TreeMap<>());
    }

    protected NumberDisplay(String line, String displayNullValue, NavigableMap<Double, String> customFormats) {
        this.line = line;
        this.displayNullValue = displayNullValue;
        this.customFormats = customFormats != null ? customFormats : new TreeMap<>();
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

    private String formatWithSuffix(double value) {
        if (customFormats.isEmpty()) {
            if (value == (long) value) {
                return String.format(Locale.ENGLISH, "%,d", (long) value);
            } else {
                return String.format(Locale.ENGLISH, "%,.2f", value).replaceAll("\\.?0+$", "");
            }
        }

        boolean isNegative = value < 0;
        double absValue = Math.abs(value);
        
        Map.Entry<Double, String> entry = customFormats.floorEntry(absValue);
        if (entry == null || entry.getKey() == 0 || absValue < 1000) {
            if (absValue == (long) absValue) {
                return (isNegative ? "-" : "") + String.format(Locale.ENGLISH, "%,d", (long) absValue);
            } else {
                String formatted = String.format(Locale.ENGLISH, "%,.2f", absValue);
                formatted = formatted.replaceAll("\\.?0+$", "");
                return (isNegative ? "-" : "") + formatted;
            }
        }

        double threshold = entry.getKey();
        String suffix = entry.getValue();
        double divided = absValue / threshold;
        if (divided < 10) {
            String formatted = String.format(Locale.ENGLISH, "%,.2f", divided);
            formatted = formatted.replaceAll("\\.?0+$", "");
            return (isNegative ? "-" : "") + formatted + suffix;
        } else if (divided < 100) {
            String formatted = String.format(Locale.ENGLISH, "%,.1f", divided);
            formatted = formatted.replaceAll("\\.?0+$", "");
            return (isNegative ? "-" : "") + formatted + suffix;
        } else {
            return (isNegative ? "-" : "") + String.format(Locale.ENGLISH, "%,.0f", divided) + suffix;
        }
    }

    @Override
    public @NotNull String getDisplayValue(@Nullable V value, @Nullable String formatQuery) {
        if (value == null) {
            return displayNullValue;
        }

        double doubleValue = value.doubleValue();

        if (formatQuery == null) {
            return formatWithSuffix(doubleValue);
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

            long time = value.longValue();
            String unitString = Optional.ofNullable(settings.get("unit")).orElse("ticks");
            long millis;
            if (unitString.equalsIgnoreCase("ticks")) {
                millis = time * 50;
            } else {
                try {
                    TimeUnit unit = TimeUnit.valueOf(unitString.toUpperCase());
                    millis = unit.toMillis(time);
                } catch (IllegalArgumentException e) {
                    return "INVALID_UNIT";
                }
            }

            String type = Optional.ofNullable(settings.get("type")).orElse("duration");
            Optional<String> patternOptional = Optional.ofNullable(settings.get("pattern"));
            if (type.equalsIgnoreCase("time")) {
                String pattern = patternOptional.orElse("RFC_1123_DATE_TIME");
                Optional<DateTimeFormatter> formatterOptional = DateTimeFormatters.getFormatter(pattern);
                if (!formatterOptional.isPresent()) {
                    return "INVALID_FORMAT";
                }
                DateTimeFormatter formatter = formatterOptional.get();

                Instant date = Instant.ofEpochMilli(millis);
                try {
                    return formatter.format(date);
                } catch (IllegalArgumentException e) {
                    return "CANNOT_FORMAT";
                }
            } else if (type.equalsIgnoreCase("duration")) {
                String pattern = patternOptional.orElse("default");
                if (pattern.equalsIgnoreCase("default")) {
                    return DurationTimeFormatters.formatDuration(millis, "HH:mm:ss");
                } else if (pattern.equalsIgnoreCase("word")) {
                    return DurationTimeFormatters.formatDurationWords(millis, true, true);
                } else if (pattern.equalsIgnoreCase("short")) {
                    return DurationTimeFormatters.formatDuration(millis, "H:mm:ss");
                } else if (pattern.equalsIgnoreCase("short-word")) {
                    return DurationTimeFormatters.formatDuration(millis, "d'd 'H'h 'm'm 's's'");
                } else {
                    return DurationTimeFormatters.formatDuration(millis, pattern);
                }
            }
        }

        try {
            if (customFormats.isEmpty()) {
                DecimalFormat decimalFormat = new DecimalFormat(formatQuery);
                return decimalFormat.format(doubleValue);
            }
            
            boolean isNegative = doubleValue < 0;
            double absValue = Math.abs(doubleValue);
            
            Map.Entry<Double, String> entry = customFormats.floorEntry(absValue);
            if (entry == null || entry.getKey() == 0) {
                DecimalFormat decimalFormat = new DecimalFormat(formatQuery);
                return (isNegative ? "-" : "") + decimalFormat.format(doubleValue);
            }

            double threshold = entry.getKey();
            String suffix = entry.getValue();
            double divided = absValue / threshold;
            DecimalFormat decimalFormat = new DecimalFormat(formatQuery);
            String formatted = decimalFormat.format(divided);
            formatted = formatted.replaceAll("\\.0*$", "");
            
            return (isNegative ? "-" : "") + formatted + suffix;
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