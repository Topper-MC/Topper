package me.hsgamer.topper.storage.bundle.converter;

import java.util.function.Function;

public class StringConverter extends SimpleValueConverter<String> {
    public StringConverter(String valueName, String sqlType) {
        super(Function.identity(), Function.identity(), valueName, sqlType);
    }

    public StringConverter(String valueName, String sqlType, boolean isStringNationalized) {
        super(Function.identity(), Function.identity(), valueName, sqlType, isStringNationalized);
    }

    public StringConverter(String valueName, boolean isStringNationalized, int stringMaxLength) {
        super(Function.identity(), Function.identity(), valueName, isStringNationalized, stringMaxLength);
    }
}
