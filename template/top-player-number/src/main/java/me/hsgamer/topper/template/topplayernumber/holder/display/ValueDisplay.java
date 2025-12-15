package me.hsgamer.topper.template.topplayernumber.holder.display;

import me.hsgamer.topper.query.display.number.NumberDisplay;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueDisplay extends NumberDisplay<UUID, Double> {
    private static final Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");

    public final String defaultLine;
    public final String displayNullName;
    public final String displayNullUuid;
    private final Function<UUID, String> nameFunction;

    public ValueDisplay(Function<UUID, String> nameFunction, Settings settings) {
        super(settings.displayNullValue());
        this.nameFunction = nameFunction;
        this.defaultLine = settings.defaultLine();
        this.displayNullName = settings.displayNullName();
        this.displayNullUuid = settings.displayNullUuid();
    }

    public @NotNull String getDisplayKey(@Nullable UUID uuid) {
        return uuid != null ? uuid.toString() : displayNullUuid;
    }

    public @NotNull String getDisplayName(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid).map(nameFunction).orElse(displayNullName);
    }

    public String getDisplayLine(int index /* 1-based */, @Nullable Map.Entry<UUID, Double> entry) {
        String line = this.defaultLine
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

    public interface Settings {
        String defaultLine();

        String displayNullName();

        String displayNullUuid();

        String displayNullValue();
    }
}
