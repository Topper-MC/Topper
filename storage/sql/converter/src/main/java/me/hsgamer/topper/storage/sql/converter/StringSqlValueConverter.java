package me.hsgamer.topper.storage.sql.converter;

import java.util.function.Function;

public class StringSqlValueConverter extends SimpleSqlValueConverter<String> {
    public StringSqlValueConverter(String valueName, String sqlType) {
        super(Function.identity(), Function.identity(), valueName, sqlType);
    }

    public StringSqlValueConverter(String valueName, String sqlType, boolean isStringNationalized) {
        super(Function.identity(), Function.identity(), valueName, sqlType, isStringNationalized);
    }

    public StringSqlValueConverter(String valueName, boolean isStringNationalized, int stringMaxLength) {
        super(Function.identity(), Function.identity(), valueName, isStringNationalized, stringMaxLength);
    }
}
