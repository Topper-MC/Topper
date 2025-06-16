package me.hsgamer.topper.spigot.plugin.holder.display;

import me.hsgamer.hscore.common.Validate;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ValueDisplay implements SimpleQueryDisplay<UUID, Double> {
    private static final Map<String, DateTimeFormatter> FORMATTER_MAP = new ConcurrentHashMap<>();
    private static final Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");
    private static final String FORMAT_QUERY_DECIMAL_FORMAT_PREFIX = "decimal:";
    private static final String FORMAT_QUERY_TIME_FORMAT_PREFIX = "time:";

    public final String line;
    public final String displayNullName;
    public final String displayNullUuid;
    public final String displayNullValue;

    public ValueDisplay(Map<String, Object> map) {
        this.line = Optional.ofNullable(map.get("line"))
                .map(Object::toString)
                .orElse("&7[&b{index}&7] &b{name} &7- &b{value}");
        this.displayNullName = Optional.ofNullable(map.get("null-name"))
                .map(Object::toString)
                .orElse("---");
        this.displayNullUuid = Optional.ofNullable(map.get("null-uuid"))
                .map(Object::toString)
                .orElse("---");
        this.displayNullValue = Optional.ofNullable(map.get("null-value"))
                .map(Object::toString)
                .orElse("---");
    }

    private static Optional<DateTimeFormatter> getFormatter(String name) {
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

    public @NotNull String getDisplayKey(@Nullable UUID uuid) {
        return uuid != null ? uuid.toString() : displayNullUuid;
    }

    public @NotNull String getDisplayName(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid)
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .orElse(displayNullName);
    }

    public @NotNull String getDisplayValue(@Nullable Double value, @Nullable String formatQuery) {
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
                    .flatMap(Validate::getNumber)
                    .map(Number::intValue)
                    .ifPresent(decimalFormat::setGroupingSize);
            Optional.ofNullable(settings.get("maximumFractionDigits"))
                    .flatMap(Validate::getNumber)
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
                Optional<DateTimeFormatter> formatterOptional = getFormatter(pattern);
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
                    return DurationFormatUtils.formatDuration(millis, "HH:mm:ss");
                } else if (pattern.equalsIgnoreCase("word")) {
                    return DurationFormatUtils.formatDurationWords(millis, true, true);
                } else if (pattern.equalsIgnoreCase("short")) {
                    return DurationFormatUtils.formatDuration(millis, "H:mm:ss");
                } else if (pattern.equalsIgnoreCase("short-word")) {
                    return DurationFormatUtils.formatDuration(millis, "d'd 'H'h 'm'm 's's'");
                } else {
                    try {
                        return DurationFormatUtils.formatDuration(millis, pattern);
                    } catch (IllegalArgumentException e) {
                        return "INVALID_FORMAT";
                    }
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

    public String getDisplayLine(int index /* 1-based */, @Nullable Map.Entry<UUID, Double> entry) {
        String line = this.line
                .replace("{index}", String.valueOf(index))
                .replace("{uuid}", getDisplayKey(entry == null ? null : entry.getKey()))
                .replace("{name}", getDisplayName(entry == null ? null : entry.getKey()));

        Double value = entry == null ? null : entry.getValue();
        Matcher matcher = VALUE_PLACEHOLDER_PATTERN.matcher(line);
        while (matcher.find()) {
            String formatType = matcher.group(1);
            line = line.replace(matcher.group(), getDisplayValue(value, formatType));
        }

        return line;
    }

    public String getDisplayLine(int index /* 1-based */, NumberTopHolder holder) {
        return getDisplayLine(index, holder.getSnapshotAgent().getSnapshotByIndex(index - 1).orElse(null));
    }
}
