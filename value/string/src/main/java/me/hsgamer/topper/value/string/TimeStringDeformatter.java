package me.hsgamer.topper.value.string;

import me.hsgamer.topper.value.timeformat.DateTimeFormatters;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.function.UnaryOperator;

public class TimeStringDeformatter implements UnaryOperator<String> {
    private final DateTimeFormatter formatter;

    public TimeStringDeformatter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    public TimeStringDeformatter(String pattern) {
        this(DateTimeFormatters.getFormatter(pattern).orElse(null));
    }

    @Override
    public String apply(String string) {
        if (formatter == null) return "NO FORMATTER";
        return Long.toString(formatter.parse(string).getLong(ChronoField.INSTANT_SECONDS));
    }
}
