package me.hsgamer.topper.agent.storage.simple.setting;

import java.io.File;

public interface DataStorageBuilderSetting {
    DatabaseSetting getDatabaseSetting();

    File getBaseFolder();
}
