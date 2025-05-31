package me.hsgamer.topper.value.string;

import java.util.function.UnaryOperator;

public class NumberStringDeformatter implements UnaryOperator<String> {
    public char decimalSeparator = '.';

    public NumberStringDeformatter(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    @Override
    public String apply(String string) {
        StringBuilder builder = new StringBuilder();
        boolean decimalSeparatorFound = false;
        for (char c : string.toCharArray()) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else if (!decimalSeparatorFound && c == decimalSeparator) {
                builder.append('.');
                decimalSeparatorFound = true;
            }
        }
        return builder.toString().trim();
    }
}
