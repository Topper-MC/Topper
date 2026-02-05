package me.hsgamer.topper.template.snapshotdisplayline;

import me.hsgamer.topper.agent.snapshot.SnapshotAgent;
import me.hsgamer.topper.query.simple.SimpleQueryDisplay;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface SnapshotDisplayLine<K, V> {
    Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");

    SimpleQueryDisplay<K, V> getDisplay();

    SnapshotAgent<K, V> getSnapshotAgent();

    String getDisplayLine();

    default String display(int index /* 1-based */) {
        SnapshotAgent<K, V> snapshot = getSnapshotAgent();
        SimpleQueryDisplay<K, V> display = getDisplay();

        Map.Entry<K, V> entry = snapshot.getSnapshotByIndex(index - 1).orElse(null);
        String line = this.getDisplayLine()
                .replace("{index}", String.valueOf(index))
                .replace("{key}", display.getDisplayKey(entry == null ? null : entry.getKey()))
                .replace("{name}", display.getDisplayName(entry == null ? null : entry.getKey()));

        V value = entry == null ? null : entry.getValue();
        Matcher matcher = VALUE_PLACEHOLDER_PATTERN.matcher(line);
        while (matcher.find()) {
            String formatType = matcher.group(1);
            line = line.replace(matcher.group(), display.getDisplayValue(value, formatType));
        }

        return line;
    }
}
