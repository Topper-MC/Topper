package me.hsgamer.topper.template.storagesupplier;

import me.hsgamer.topper.template.storagesupplier.storage.DataStorageSupplier;

public interface StorageSupplierTemplate {
    DataStorageSupplier getDataStorageSupplier(Settings settings);

    interface Settings {
        String storageType();

        DataStorageSupplier.Settings storageSettings();
    }
}
