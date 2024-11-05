package me.hsgamer.topper.spigot.plugin.command;

import me.hsgamer.hscore.bukkit.utils.MessageUtils;
import me.hsgamer.topper.spigot.plugin.Permissions;
import me.hsgamer.topper.spigot.plugin.TopperPlugin;
import me.hsgamer.topper.spigot.plugin.config.MessageConfig;
import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import me.hsgamer.topper.spigot.plugin.holder.display.ValueDisplay;
import me.hsgamer.topper.spigot.plugin.manager.TopManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.hsgamer.hscore.bukkit.utils.MessageUtils.sendMessage;

public class GetTopListCommand extends Command {
    private final TopperPlugin instance;

    public GetTopListCommand(TopperPlugin instance) {
        super("gettoplist", "Get Top List", "/gettop <holder> [from_index] [to_index]", Arrays.asList("toplist", "gettop"));
        this.instance = instance;
        setPermission(Permissions.TOP.getName());
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!testPermission(sender)) {
            return false;
        }
        if (args.length < 1) {
            sendMessage(sender, "&c" + getUsage());
            return false;
        }
        Optional<NumberTopHolder> optional = instance.get(TopManager.class).getTopHolder(args[0]);
        if (!optional.isPresent()) {
            MessageUtils.sendMessage(sender, instance.get(MessageConfig.class).getTopHolderNotFound());
            return false;
        }
        NumberTopHolder topHolder = optional.get();

        int fromIndex = 1;
        int toIndex = 10;
        if (args.length == 2) {
            try {
                toIndex = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sendMessage(sender, instance.get(MessageConfig.class).getNumberRequired());
                return false;
            }
        } else if (args.length > 2) {
            try {
                fromIndex = Integer.parseInt(args[1]);
                toIndex = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sendMessage(sender, instance.get(MessageConfig.class).getNumberRequired());
                return false;
            }
        }
        if (fromIndex >= toIndex) {
            sendMessage(sender, instance.get(MessageConfig.class).getIllegalFromToIndex());
            return false;
        }

        ValueDisplay valueDisplay = topHolder.getValueDisplay();
        List<String> topList = IntStream.rangeClosed(fromIndex, toIndex).mapToObj(index -> valueDisplay.getDisplayLine(index, topHolder)).collect(Collectors.toList());
        if (topList.isEmpty()) {
            sendMessage(sender, instance.get(MessageConfig.class).getTopEmpty());
        } else {
            topList.forEach(s -> sendMessage(sender, s));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return instance.get(TopManager.class).getTopHolderNames().stream()
                    .filter(name -> args[0].isEmpty() || name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 || args.length == 3) {
            return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        }
        return Collections.emptyList();
    }
}
