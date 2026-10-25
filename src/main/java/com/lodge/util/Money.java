package com.lodge.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small helper for all Zambian Kwacha (K) money handling in the system.
 * Centralising formatting keeps every screen and receipt consistent.
 */
public final class Money {

    public static final String SYMBOL = "K";

    private Money() {
        // utility class
    }

    /** Rounds to 2 decimals using HALF_UP so totals are always currency-safe. */
    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Formats a double as "K 1,234.50". */
    public static String format(double value) {
        return String.format("%s %,.2f", SYMBOL, value);
    }

    /**
     * Parses user input such as "1200", "1,200.50" or "K 1200".
     *
     * @throws NumberFormatException when the text is not a valid amount
     */
    public static double parse(String text) {
        if (text == null) {
            throw new NumberFormatException("Amount is empty");
        }
        String cleaned = text.replace(SYMBOL, "").replace(",", "").trim();
        if (cleaned.isEmpty()) {
            throw new NumberFormatException("Amount is empty");
        }
        return round(Double.parseDouble(cleaned));
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
