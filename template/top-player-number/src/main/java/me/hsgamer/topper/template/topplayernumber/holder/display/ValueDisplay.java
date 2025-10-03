package me.hsgamer.topper.template.topplayernumber.holder.display;

import me.hsgamer.topper.query.display.number.NumberDisplay;
import me.hsgamer.topper.template.topplayernumber.holder.NumberTopHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class ValueDisplay extends NumberDisplay<UUID, Double> {
    public final String displayNullName;
    public final String displayNullUuid;
    private final Function<UUID, String> nameFunction;

    public ValueDisplay(Function<UUID, String> nameFunction, Settings settings) {
        super(settings.defaultLine(), settings.displayNullValue());
        this.nameFunction = nameFunction;
        this.displayNullName = settings.displayNullName();
        this.displayNullUuid = settings.displayNullUuid();
    }

    public @NotNull String getDisplayKey(@Nullable UUID uuid) {
        return uuid != null ? uuid.toString() : displayNullUuid;
    }

    public @NotNull String getDisplayName(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid).map(nameFunction).orElse(displayNullName);
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
