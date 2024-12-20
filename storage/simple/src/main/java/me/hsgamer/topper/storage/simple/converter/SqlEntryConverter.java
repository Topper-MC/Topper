package me.hsgamer.topper.storage.simple.converter;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SqlEntryConverter<K, V> {
    String[] getKeyColumns();

    String[] getValueColumns();

    String[] getKeyColumnDefinitions();

    String[] getValueColumnDefinitions();

    Object[] toKeyQueryValues(K key);

    Object[] toValueQueryValues(V value);

    K getKey(ResultSet resultSet) throws SQLException;

    V getValue(ResultSet resultSet) throws SQLException;
}
