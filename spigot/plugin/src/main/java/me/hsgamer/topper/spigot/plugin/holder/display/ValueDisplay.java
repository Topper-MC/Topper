package me.hsgamer.topper.spigot.plugin.holder.display;

import me.hsgamer.topper.query.simple.SimpleQueryDisplay;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ValueDisplay implements SimpleQueryDisplay<UUID, Double> {
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
            Optional.ofNullable(settings.get("decimalSeparator"))
                    .map(s -> s.charAt(0))
                    .ifPresent(symbols::setDecimalSeparator);
            Optional.ofNullable(settings.get("groupingSeparator"))
                    .map(s -> s.charAt(0))
                    .ifPresent(symbols::setGroupingSeparator);

            DecimalFormat decimalFormat = new DecimalFormat();
            decimalFormat.setDecimalFormatSymbols(symbols);
            decimalFormat.setRoundingMode(RoundingMode.HALF_EVEN);
            Optional.ofNullable(settings.get("maximumFractionDigits"))
                    .map(Integer::parseInt)
                    .ifPresent(decimalFormat::setMaximumFractionDigits);
            return decimalFormat.format(value);
        }

        if (formatQuery.startsWith(FORMAT_QUERY_TIME_FORMAT_PREFIX)) {
            Map<String, String> settings = getSettings(formatQuery.substring(FORMAT_QUERY_TIME_FORMAT_PREFIX.length()));

            String pattern = Optional.ofNullable(settings.get("pattern")).orElse("HH:mm:ss");
            String type = Optional.ofNullable(settings.get("type")).orElse("duration");
            long time = value.longValue();
            TimeUnit unit = Optional.ofNullable(settings.get("unit"))
                    .map(s -> {
                        try {
                            return TimeUnit.valueOf(s.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .orElse(TimeUnit.SECONDS);
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
                try {
                    return DurationFormatUtils.formatDuration(millis, pattern);
                } catch (IllegalArgumentException e) {
                    return "INVALID_FORMAT";
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
