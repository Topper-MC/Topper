package me.hsgamer.topper.storage.simple.converter;

import java.util.function.Function;

public class StringConverter extends SimpleValueConverter<String> {
    public StringConverter(String valueName, boolean isStringNative, int stringMaxLength) {
        super(Function.identity(), Function.identity(), valueName, isStringNative, stringMaxLength);
    }
}
