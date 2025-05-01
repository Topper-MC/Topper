package me.hsgamer.topper.spigot.plugin.util;

public interface ParseUtil {
    static double parsePlaceholderNumber(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.join(" ",
                            "There is a problem when parsing your placeholder.",
                            "This is usually not a Topper's issue, but an issue with your placeholder.",
                            "Take a look at the wiki and check against your placeholder settings.",
                            "https://topper-mc.github.io/Wiki/spigot/faq.html"
                    ),
                    e
            );
        }
    }
}
