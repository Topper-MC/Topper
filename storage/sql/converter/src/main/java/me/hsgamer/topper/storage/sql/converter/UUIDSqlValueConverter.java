package me.hsgamer.topper.storage.sql.converter;

import java.util.UUID;

public class UUIDSqlValueConverter extends SimpleSqlValueConverter<UUID> {
    public UUIDSqlValueConverter(String valueName) {
        super(UUID::toString, s -> {
            try {
                return UUID.fromString(s);
            } catch (Exception e) {
                return null;
            }
        }, valueName, false, 36);
    }
}
