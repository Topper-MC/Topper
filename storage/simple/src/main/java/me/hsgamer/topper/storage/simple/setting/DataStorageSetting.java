package me.hsgamer.topper.storage.simple.setting;

import me.hsgamer.hscore.database.Setting;

import java.io.File;
import java.util.function.Consumer;

public interface DataStorageSetting {
    Consumer<Setting> getDatabaseSettingModifier();

    File getBaseFolder();
}
