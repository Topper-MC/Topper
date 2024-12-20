package me.hsgamer.topper.storage.number;

import me.hsgamer.topper.storage.simple.converter.SqlEntryConverter;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlNumberEntryConverter<K> extends SqlEntryConverter<K, Double> {
    @Override
    default String[] getValueColumns() {
        return new String[]{"value"};
    }

    @Override
    default String[] getValueColumnDefinitions() {
        return new String[]{"`value` double DEFAULT 0"};
    }

    @Override
    default Object[] toValueQueryValues(Double value) {
        return new Object[]{value};
    }

    @Override
    default Double getValue(ResultSet resultSet) throws SQLException {
        return resultSet.getDouble("value");
    }
}
