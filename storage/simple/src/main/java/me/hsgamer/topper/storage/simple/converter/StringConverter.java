package me.hsgamer.topper.storage.simple.converter;

import java.util.function.Function;

public class StringConverter extends SimpleValueConverter<String> {
    public StringConverter(String valueName, String sqlType) {
        super(Function.identity(), Function.identity(), valueName, sqlType);
    }

    public StringConverter(String valueName, boolean isStringNationalized, int stringMaxLength) {
        super(Function.identity(), Function.identity(), valueName, isStringNationalized, stringMaxLength);
    }
}
