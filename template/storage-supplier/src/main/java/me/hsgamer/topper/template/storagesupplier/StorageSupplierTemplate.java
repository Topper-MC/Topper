package me.hsgamer.topper.template.storagesupplier;

import me.hsgamer.topper.storage.sql.core.SqlDatabaseSetting;
import me.hsgamer.topper.template.storagesupplier.storage.DataStorageSupplier;

import java.io.File;

public interface StorageSupplierTemplate {
    DataStorageSupplier getDataStorageSupplier(Settings settings);

    interface Settings {
        String storageType();

        SqlDatabaseSetting databaseSetting();

        File baseFolder();
    }
}
