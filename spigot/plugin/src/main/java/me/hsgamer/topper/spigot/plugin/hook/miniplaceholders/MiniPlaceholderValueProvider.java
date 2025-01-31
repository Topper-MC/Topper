package me.hsgamer.topper.spigot.plugin.hook.miniplaceholders;

import io.github.miniplaceholders.api.MiniPlaceholders;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.holder.provider.NumberStringValueProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MiniPlaceholderValueProvider extends NumberStringValueProvider {
    private final String placeholder;

    public MiniPlaceholderValueProvider(TopperPlugin plugin, Map<String, Object> map) {
        super(plugin, map);
        placeholder = Optional.ofNullable(map.get("placeholder")).map(Object::toString).orElse("");
    }

    @Override
    protected String getDisplayName() {
        return placeholder;
    }

    @Override
    protected Optional<String> getString(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player == null) {
            return Optional.empty();
        }

        TagResolver resolver = MiniPlaceholders.getAudiencePlaceholders(player);

        Component component = MiniMessage.miniMessage().deserialize(placeholder, resolver);
        String parsed = PlainTextComponentSerializer.plainText().serialize(component).trim();

        return Optional.of(parsed);
    }
}
