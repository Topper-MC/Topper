package me.hsgamer.topper.spigot.plugin.holder.provider;

import io.github.projectunified.minelib.scheduler.async.AsyncScheduler;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.hscore.common.MapUtils;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public abstract class NumberStringValueProvider implements ValueProvider {
    protected final TopperPlugin plugin;
    private final boolean isAsync;
    private final boolean showErrors;
    private final boolean isFormatted;
    private final FormattedSettings formattedSettings;

    public NumberStringValueProvider(TopperPlugin plugin, Map<String, Object> map) {
        this.plugin = plugin;
        isAsync = Optional.ofNullable(map.get("async"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        showErrors = Optional.ofNullable(map.get("show-errors"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        isFormatted = Optional.ofNullable(map.get("formatted"))
                .map(Object::toString)
                .map(String::toLowerCase)
                .map(Boolean::parseBoolean)
                .orElse(false);
        formattedSettings = new FormattedSettings(
                Optional.ofNullable(map.get("formatted-settings"))
                        .flatMap(MapUtils::castOptionalStringObjectMap)
                        .orElseGet(Collections::emptyMap)
        );
    }

    protected abstract String getDisplayName();

    protected abstract ValueState getString(UUID uuid);

    @Override
    public CompletableFuture<Optional<Double>> getValue(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ValueState valueState = getString(uuid);
                if (!valueState.handled) {
                    return Optional.empty();
                }

                Optional<String> value = Optional.ofNullable(valueState.value)
                        .filter(s -> !s.isEmpty())
                        .map(s -> isFormatted ? formattedSettings.clearFormat(s) : s);
                if (!value.isPresent()) {
                    if (showErrors) {
                        plugin.getLogger().warning("The value of " + getDisplayName() + " is empty");
                    }
                    return Optional.empty();
                }

                return Optional.of(Double.parseDouble(value.get()));
            } catch (Exception e) {
                if (showErrors) {
                    plugin.getLogger().log(Level.WARNING, "There is an error while parsing the value of " + getDisplayName(), e);
                }
                return Optional.empty();
            }
        }, (isAsync ? AsyncScheduler.get(plugin) : GlobalScheduler.get(plugin)).getExecutor());
    }

    private static final class FormattedSettings {
        private final char decimalSeparator;

        private FormattedSettings(Map<String, Object> map) {
            decimalSeparator = Optional.ofNullable(map.get("decimal-separator"))
                    .map(Object::toString)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.charAt(0))
                    .orElse('.');
        }

        String clearFormat(String string) {
            StringBuilder builder = new StringBuilder();
            boolean decimalSeparatorFound = false;
            for (char c : string.toCharArray()) {
                if (Character.isDigit(c)) {
                    builder.append(c);
                } else if (!decimalSeparatorFound && c == decimalSeparator) {
                    builder.append('.');
                    decimalSeparatorFound = true;
                }
            }
            return builder.toString();
        }
    }

    public static class ValueState {
        public final boolean handled;
        public final @Nullable String value;

        private ValueState(boolean handled, @Nullable String value) {
            this.handled = handled;
            this.value = value;
        }

        public static ValueState handled(@Nullable String value) {
            return new ValueState(true, value);
        }

        public static ValueState unhandled() {
            return new ValueState(false, null);
        }
    }
}
