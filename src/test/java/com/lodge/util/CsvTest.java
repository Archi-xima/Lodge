package com.lodge.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CsvTest {
    @Test void quotesNamesCommasQuotesAndLineBreaks() {
        assertEquals("\"Guest, Inc.\",\"A \"\"quoted\"\" note\",\"line1\nline2\"\r\n",
                Csv.row("Guest, Inc.", "A \"quoted\" note", "line1\nline2"));
    }

    @Test void untrustedTextCannotBecomeASpreadsheetFormula() {
        assertEquals("\"'=1+1\",\"'  @SUM(A1)\",\"'+260977123456\"\r\n",
                Csv.row("=1+1", "  @SUM(A1)", "+260977123456"));
    }

    @Test void amountsRemainNumericIncludingNegativeBalances() {
        assertEquals("1250.5,-38.0,\"\"\r\n", Csv.row(1250.5, -38.0, null));
    }
}
