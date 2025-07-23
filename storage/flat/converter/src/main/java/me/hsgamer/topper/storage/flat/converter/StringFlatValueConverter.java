package me.hsgamer.topper.storage.flat.converter;

import java.util.function.Function;

public class StringFlatValueConverter extends SimpleFlatValueConverter<String> {
    public StringFlatValueConverter() {
        super(Function.identity(), Function.identity());
    }
}
