package me.hsgamer.topper.storage.flat.converter;

import java.util.UUID;

public class UUIDFlatValueConverter extends SimpleFlatValueConverter<UUID> {
    public UUIDFlatValueConverter() {
        super(UUID::toString, s -> {
            try {
                return UUID.fromString(s);
            } catch (Exception e) {
                return null;
            }
        });
    }
}
