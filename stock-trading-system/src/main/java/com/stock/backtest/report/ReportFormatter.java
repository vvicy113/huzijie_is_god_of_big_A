package com.stock.backtest.report;

public class ReportFormatter {

    public static String separator(int width) {
        return "=".repeat(width);
    }

    public static String line(int width) {
        return "-".repeat(width);
    }

    public static String formatRow(String label, String value, int labelWidth) {
        return String.format("  %-" + labelWidth + "s : %s", label, value);
    }

    public static String formatPct(double value) {
        return String.format("%.2f%%", value);
    }

    public static String formatMoney(double value) {
        return String.format("¥%.2f", value);
    }

    public static String formatDate(String prefix, java.time.LocalDate start, java.time.LocalDate end) {
        return prefix + ": " + start + " ~ " + end;
    }
}
