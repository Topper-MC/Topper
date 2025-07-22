package me.hsgamer.topper.storage.bundle.converter;

import java.util.UUID;

public class UUIDConverter extends SimpleValueConverter<UUID> {
    public UUIDConverter(String valueName) {
        super(UUID::toString, s -> {
            try {
                return UUID.fromString(s);
            } catch (Exception e) {
                return null;
            }
        }, valueName, false, 36);
    }
}
