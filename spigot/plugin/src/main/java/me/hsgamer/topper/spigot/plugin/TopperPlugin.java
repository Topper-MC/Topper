package me.hsgamer.topper.spigot.plugin;

import io.github.projectunified.minelib.plugin.base.BasePlugin;
import io.github.projectunified.minelib.plugin.command.CommandComponent;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.hscore.bukkit.utils.MessageUtils;
import me.hsgamer.hscore.checker.spigotmc.SpigotVersionChecker;
import me.hsgamer.hscore.common.CollectionUtils;
import me.hsgamer.hscore.common.MapUtils;
import me.hsgamer.hscore.config.Config;
import me.hsgamer.hscore.config.PathString;
import me.hsgamer.hscore.config.proxy.ConfigGenerator;
import me.hsgamer.topper.spigot.plugin.builder.NumberStorageBuilder;
import me.hsgamer.topper.spigot.plugin.builder.ValueProviderBuilder;
import me.hsgamer.topper.spigot.plugin.command.GetTopListCommand;
import me.hsgamer.topper.spigot.plugin.command.ReloadCommand;
import me.hsgamer.topper.spigot.plugin.config.DatabaseConfig;
import me.hsgamer.topper.spigot.plugin.config.MainConfig;
import me.hsgamer.topper.spigot.plugin.config.MessageConfig;
import me.hsgamer.topper.spigot.plugin.hook.HookSystem;
import me.hsgamer.topper.spigot.plugin.listener.JoinListener;
import me.hsgamer.topper.spigot.plugin.manager.TopManager;
import me.hsgamer.topper.spigot.plugin.manager.TopQueryManager;
import me.hsgamer.varblocks.VarBlocks;
import me.hsgamer.varblocks.api.BlockEntry;
import me.hsgamer.varblocks.manager.BlockManager;
import me.hsgamer.varblocks.manager.TemplateManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopperPlugin extends BasePlugin {
    @Override
    protected List<Object> getComponents() {
        return Arrays.asList(
                new NumberStorageBuilder(
                        this,
                        new File(getDataFolder(), "top"),
                        () -> ConfigGenerator.newInstance(DatabaseConfig.class, new BukkitConfig(this, "database.yml")).toDatabaseSetting()
                ),
                new ValueProviderBuilder(),
                ConfigGenerator.newInstance(MainConfig.class, new BukkitConfig(this)),
                ConfigGenerator.newInstance(MessageConfig.class, new BukkitConfig(this, "messages.yml")),

                new HookSystem(this),

                new TopManager(this),
                new TopQueryManager(this),

                new Permissions(this),
                new CommandComponent(this,
                        new ReloadCommand(this),
                        new GetTopListCommand(this)
                ),
                new JoinListener(this)
        );
    }

    @Override
    public void load() {
        MessageUtils.setPrefix(get(MessageConfig.class)::getPrefix);
        migrateConfig();
    }

    @Override
    public void enable() {
        new Metrics(this, 14938);
        if (getDescription().getVersion().contains("SNAPSHOT")) {
            getLogger().warning("You are using the development version");
            getLogger().warning("This is not ready for production");
            getLogger().warning("Use in your own risk");
        } else {
            new SpigotVersionChecker(101325).getVersion().whenComplete((output, throwable) -> {
                if (throwable != null) {
                    getLogger().log(Level.WARNING, "Failed to check spigot version", throwable);
                } else if (output != null) {
                    if (this.getDescription().getVersion().equalsIgnoreCase(output)) {
                        getLogger().info("You are using the latest version");
                    } else {
                        getLogger().warning("There is an available update");
                        getLogger().warning("New Version: " + output);
                    }
                }
            });
        }
    }

    private void migrateConfig() {
        MainConfig mainConfig = get(MainConfig.class);
        Config config = mainConfig.getConfig();

        Map<String[], Object> placeholders = config.getValues(false, "placeholders");
        if (!placeholders.isEmpty()) {
            Map<String, Map<String, Object>> holders = new HashMap<>();
            Pattern placeholderPattern = Pattern.compile("\\s*(\\[.*])?\\s*(.*)\\s*");
            for (Map.Entry<String[], Object> entry : placeholders.entrySet()) {
                String key = PathString.joinDefault(entry.getKey());
                String value = entry.getValue().toString();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("type", "placeholder");
                Matcher matcher = placeholderPattern.matcher(value);
                if (matcher.matches()) {
                    String placeholder = Optional.ofNullable(matcher.group(2)).orElse("");
                    String prefix = Optional.ofNullable(matcher.group(1)).map(String::toLowerCase).orElse("");
                    map.put("placeholder", placeholder);
                    map.put("online", prefix.contains("[online]"));
                    map.put("async", prefix.contains("[async]"));
                    map.put("lenient", prefix.contains("[lenient]"));
                } else {
                    map.put("placeholder", value);
                }
                holders.put(key, map);
            }
            config.set(holders, "holders");
            config.set(null, "placeholders");
            config.save();
            getLogger().info("The config has been migrated");
            mainConfig.reloadConfig();
        }

        List<String> signs = new ArrayList<>();
        File signFile = new File(getDataFolder(), "sign.yml");
        if (signFile.exists()) {
            Config signConfig = new BukkitConfig(signFile);
            signConfig.setup();
            signs.addAll(CollectionUtils.createStringListFromObject(signConfig.getNormalized("entries")));
        }

        List<String> skulls = new ArrayList<>();
        File skullFile = new File(getDataFolder(), "skull.yml");
        if (skullFile.exists()) {
            Config skullConfig = new BukkitConfig(skullFile);
            skullConfig.setup();
            skulls.addAll(CollectionUtils.createStringListFromObject(skullConfig.getNormalized("entries")));
        }

        Map<String, Map<String, Object>> formatters = new HashMap<>();
        config.getNormalizedValues(false, "formatters")
                .forEach(
                        (key, value) ->
                                MapUtils.castOptionalStringObjectMap(value).ifPresent(map -> formatters.put(key[0], map))
                );

        List<String> signLines = Arrays.asList(
                "&6&m               ",
                "&b#{index} &a{name}",
                "&a{value} {suffix}",
                "&6&m               "
        );
        Config messageConfig = get(MessageConfig.class).getConfig();
        if (messageConfig.contains("sign-lines")) {
            signLines = CollectionUtils.createStringListFromObject(messageConfig.getNormalized("sign-lines"));
        }

        boolean blockMigrateSuccess = false;

        if (signs.isEmpty() && skulls.isEmpty()) {
            blockMigrateSuccess = true;
        } else if (Bukkit.getPluginManager().getPlugin("VarBlocks") != null) {
            VarBlocks varBlocks = JavaPlugin.getPlugin(VarBlocks.class);

            TemplateManager templateManager = varBlocks.get(TemplateManager.class);
            if (templateManager.getTemplate("topper-sign").isEmpty()) {
                List<String> newSignLines = new ArrayList<>(signLines);
                newSignLines.replaceAll(s -> s
                        .replace("{uuid}", "%topper_{holder};top_key;{index}%")
                        .replace("{name}", "%topper_{holder};top_name;{index}%")
                        .replace("{value}", "%topper_{holder};top_value;{index};{format}%")
                        .replace("{value_raw}", "%topper_{holder};top_value_raw;{index}%")
                );
                templateManager.saveTemplate("topper-sign", newSignLines);
            }
            if (templateManager.getTemplate("topper-skull").isEmpty()) {
                templateManager.saveTemplate("topper-skull", Collections.singletonList("%topper_{holder};top_key;{index}%"));
            }

            BlockManager blockManager = varBlocks.get(BlockManager.class);
            for (int signIndex = 0; signIndex < signs.size(); signIndex++) {
                String sign = signs.get(signIndex);
                String[] split = sign.split(",");
                String world = split[0];
                int x = (int) Double.parseDouble(split[1]);
                int y = (int) Double.parseDouble(split[2]);
                int z = (int) Double.parseDouble(split[3]);
                String holder = split[4];
                int index = Integer.parseInt(split[5]) + 1;

                Map<String, String> arguments = new LinkedHashMap<>();

                Map<String, Object> formatter = formatters.getOrDefault(holder, Collections.emptyMap());

                int fractionDigits = Optional.ofNullable(formatter.get("fraction-digits")).map(String::valueOf).map(Integer::parseInt).orElse(-1);
                char decimalSeparator = Optional.ofNullable(formatter.get("decimal-separator")).map(String::valueOf).map(s -> s.charAt(0)).orElse('.');
                char groupSeparator = Optional.ofNullable(formatter.get("group-separator")).map(String::valueOf).map(s -> s.charAt(0)).orElse(',');
                boolean showGroupSeparator = Optional.ofNullable(formatter.get("show-group-separator")).map(String::valueOf).map(Boolean::parseBoolean).orElse(true);
                StringBuilder decimalFormatBuilder = new StringBuilder();
                if (showGroupSeparator) {
                    decimalFormatBuilder.append("#").append(groupSeparator).append("###");
                } else {
                    decimalFormatBuilder.append("0");
                }
                if (fractionDigits > 0) {
                    decimalFormatBuilder.append(decimalSeparator);
                    for (int i = 0; i < fractionDigits; i++) {
                        decimalFormatBuilder.append("#");
                    }
                }
                String decimalFormat = decimalFormatBuilder.toString();
                String displayName = Optional.ofNullable(formatter.get("display-name")).map(String::valueOf).orElse("");
                String prefix = Optional.ofNullable(formatter.get("prefix")).map(String::valueOf).orElse("");
                String suffix = Optional.ofNullable(formatter.get("suffix")).map(String::valueOf).orElse("");

                arguments.put("index", String.valueOf(index));
                arguments.put("display_name", displayName);
                arguments.put("prefix", prefix);
                arguments.put("suffix", suffix);
                arguments.put("format", decimalFormat);
                arguments.put("holder", holder);

                BlockEntry blockEntry = new BlockEntry(world, x, y, z, "sign", "topper-sign", arguments);
                blockManager.setBlockEntry("topper-sign-" + signIndex, blockEntry);
            }
            for (int skullIndex = 0; skullIndex < skulls.size(); skullIndex++) {
                String skull = skulls.get(skullIndex);
                String[] split = skull.split(",");
                String world = split[0];
                int x = (int) Double.parseDouble(split[1]);
                int y = (int) Double.parseDouble(split[2]);
                int z = (int) Double.parseDouble(split[3]);
                String holder = split[4];
                int index = Integer.parseInt(split[5]) + 1;

                Map<String, String> arguments = new LinkedHashMap<>();
                arguments.put("index", String.valueOf(index));
                arguments.put("holder", holder);

                BlockEntry blockEntry = new BlockEntry(world, x, y, z, "skull", "topper-skull", arguments);
                blockManager.setBlockEntry("topper-skull-" + skullIndex, blockEntry);
            }

            blockMigrateSuccess = true;
        } else {
            getLogger().warning("The plugin VarBlocks is not found");
            getLogger().warning("Please install it to migrate the block config");
            getLogger().warning("Link: https://www.spigotmc.org/resources/varblocks.120327/");
        }

        if (blockMigrateSuccess) {
            boolean deleteSuccess = false;
            if (signFile.exists()) {
                signFile.delete();
                deleteSuccess = true;
            }
            if (skullFile.exists()) {
                skullFile.delete();
                deleteSuccess = true;
            }
            if (deleteSuccess) {
                getLogger().info("The block config has been migrated");
            }
            config.remove("formatters");
            config.save();
            mainConfig.reloadConfig();
        }
    }
}
