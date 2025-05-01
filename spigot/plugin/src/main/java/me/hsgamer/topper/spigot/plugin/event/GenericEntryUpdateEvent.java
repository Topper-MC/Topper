package me.hsgamer.topper.spigot.plugin.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GenericEntryUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String group;
    private final String holder;
    private final UUID uuid;
    private final @Nullable Double oldValue;
    private final @Nullable Double value;

    public GenericEntryUpdateEvent(String group, String holder, UUID uuid, @Nullable Double oldValue, @Nullable Double value, boolean async) {
        super(async);
        this.group = group;
        this.holder = holder;
        this.uuid = uuid;
        this.oldValue = oldValue;
        this.value = value;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public String getGroup() {
        return group;
    }

    public String getHolder() {
        return holder;
    }

    public UUID getUuid() {
        return uuid;
    }

    public @Nullable Double getOldValue() {
        return oldValue;
    }

    public @Nullable Double getValue() {
        return value;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
