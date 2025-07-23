package me.hsgamer.topper.value.timeformat;

import java.lang.reflect.Method;

public final class DurationTimeFormatters {
    private static final Method FORMAT_DURATION_METHOD;
    private static final Method FORMAT_DURATION_WORDS_METHOD;

    static {
        Class<?> clazz;
        try {
            clazz = Class.forName("org.apache.commons.lang3.time.DurationFormatUtils");
        } catch (ClassNotFoundException e) {
            try {
                clazz = Class.forName("org.apache.commons.lang.time.DurationFormatUtils");
            } catch (ClassNotFoundException ex) {
                clazz = null;
            }
        }

        Method method = null;
        if (clazz != null) {
            try {
                method = clazz.getMethod("formatDuration", long.class, String.class);
            } catch (NoSuchMethodException e) {
                // Method not found, will return null
            }
        }

        FORMAT_DURATION_METHOD = method;

        Method wordsMethod = null;
        if (clazz != null) {
            try {
                wordsMethod = clazz.getMethod("formatDurationWords", long.class, boolean.class, boolean.class);
            } catch (NoSuchMethodException e) {
                // Method not found, will return null
            }
        }

        FORMAT_DURATION_WORDS_METHOD = wordsMethod;
    }

    private DurationTimeFormatters() {
        // Prevent instantiation
    }

    public static String formatDuration(long durationMillis, String format) {
        if (FORMAT_DURATION_METHOD != null) {
            try {
                return (String) FORMAT_DURATION_METHOD.invoke(null, durationMillis, format);
            } catch (Exception e) {
                return "INVALID FORMAT";
            }
        } else {
            return "NOT SUPPORTED";
        }
    }

    public static String formatDurationWords(long durationMillis, boolean suppressLeadingZeroElements, boolean suppressTrailingZeroElements) {
        if (FORMAT_DURATION_WORDS_METHOD != null) {
            try {
                return (String) FORMAT_DURATION_WORDS_METHOD.invoke(null, durationMillis, suppressLeadingZeroElements, suppressTrailingZeroElements);
            } catch (Exception e) {
                return "INVALID FORMAT";
            }
        } else {
            return "NOT SUPPORTED";
        }
    }
}
