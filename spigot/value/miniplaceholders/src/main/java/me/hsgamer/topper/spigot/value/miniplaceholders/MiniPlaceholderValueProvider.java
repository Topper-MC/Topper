package me.hsgamer.topper.spigot.value.miniplaceholders;

import io.github.miniplaceholders.api.MiniPlaceholders;
import me.hsgamer.topper.value.core.ValueProvider;
import me.hsgamer.topper.value.core.ValueWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MiniPlaceholderValueProvider implements ValueProvider<Player, String> {
    private final String placeholder;

    public MiniPlaceholderValueProvider(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    public @NotNull ValueWrapper<String> apply(@NotNull Player key) {
        try {
            Component component = MiniMessage.miniMessage().deserialize(placeholder,
                    MiniPlaceholders.getAudiencePlaceholders(key),
                    MiniPlaceholders.getGlobalPlaceholders()
            );
            String parsed = PlainTextComponentSerializer.plainText().serialize(component).trim();
            return ValueWrapper.handled(parsed);
        } catch (Exception e) {
            return ValueWrapper.error("Error while parsing the placeholder: " + placeholder, e);
        }
    }
}
