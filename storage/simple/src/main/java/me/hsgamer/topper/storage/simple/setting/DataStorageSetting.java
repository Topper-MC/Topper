package me.hsgamer.topper.storage.simple.setting;

import java.io.File;

public interface DataStorageSetting {
    DatabaseSetting getDatabaseSetting();

    File getBaseFolder();
}
