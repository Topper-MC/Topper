package me.hsgamer.topper.spigot.plugin.event;

import me.hsgamer.topper.core.DataEntry;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class TopEntryChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final DataEntry<UUID, Double> entry;
    private final Type type;

    public TopEntryChangeEvent(DataEntry<UUID, Double> entry, Type type) {
        this.entry = entry;
        this.type = type;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public DataEntry<UUID, Double> getEntry() {
        return entry;
    }

    public Type getType() {
        return type;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public enum Type {
        ADD,
        REMOVE,
        UPDATE
    }
}
