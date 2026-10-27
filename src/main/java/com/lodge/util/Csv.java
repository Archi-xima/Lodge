package com.lodge.util;

import java.util.stream.Collectors;

/** CSV escaping, including protection against spreadsheet formulas in entered text. */
public final class Csv {
    private Csv() { }

    public static String row(Object... cells) {
        return java.util.Arrays.stream(cells).map(Csv::cell).collect(Collectors.joining(",")) + "\r\n";
    }

    private static String cell(Object value) {
        if (value == null) return "\"\"";
        if (value instanceof Number) return value.toString();
        String text = value.toString();
        String stripped = text.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
