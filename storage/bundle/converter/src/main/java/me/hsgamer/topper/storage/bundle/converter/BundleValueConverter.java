package me.hsgamer.topper.storage.bundle.converter;

import me.hsgamer.topper.storage.flat.core.FlatValueConverter;
import me.hsgamer.topper.storage.sql.core.SqlValueConverter;

public interface BundleValueConverter<T> extends FlatValueConverter<T>, SqlValueConverter<T> {
}
