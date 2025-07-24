package me.hsgamer.topper.storage.flat.converter;

public class BooleanFlatValueConverter extends SimpleFlatValueConverter<Boolean> {
    public BooleanFlatValueConverter() {
        super(b -> Boolean.toString(b), Boolean::parseBoolean);
    }
}
