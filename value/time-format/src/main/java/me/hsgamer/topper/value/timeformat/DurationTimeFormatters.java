package me.hsgamer.topper.value.timeformat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class DurationTimeFormatters {
    private static final MethodHandle FORMAT_DURATION_METHOD;
    private static final MethodHandle FORMAT_DURATION_WORDS_METHOD;

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

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle method = null;
        if (clazz != null) {
            try {
                method = lookup.findStatic(clazz, "formatDuration", MethodType.methodType(String.class, long.class, String.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Method not found, will return null
            }
        }

        FORMAT_DURATION_METHOD = method;

        MethodHandle wordsMethod = null;
        if (clazz != null) {
            try {
                wordsMethod = lookup.findStatic(clazz, "formatDurationWords", MethodType.methodType(String.class, long.class, boolean.class, boolean.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
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
                return (String) FORMAT_DURATION_METHOD.invokeExact(durationMillis, format);
            } catch (Throwable e) {
                return "INVALID FORMAT";
            }
        } else {
            return "NOT SUPPORTED";
        }
    }

    public static String formatDurationWords(long durationMillis, boolean suppressLeadingZeroElements, boolean suppressTrailingZeroElements) {
        if (FORMAT_DURATION_WORDS_METHOD != null) {
            try {
                return (String) FORMAT_DURATION_WORDS_METHOD.invokeExact(durationMillis, suppressLeadingZeroElements, suppressTrailingZeroElements);
            } catch (Throwable e) {
                return "INVALID FORMAT";
            }
        } else {
            return "NOT SUPPORTED";
        }
    }
}
