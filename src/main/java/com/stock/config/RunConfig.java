package com.stock.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Properties;

/**
 * 运行配置——从 run.properties 读取，替代交互式菜单。
 * <p>
 * 文件不存在时返回全默认值（进入交互模式）。
 */
public class RunConfig {

    private final String mode;
    private final String strategyName;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final String primaryCode;
    private final String compareCodes;
    private final String importYear;
    private final int[] selectorCodes;
    private final int[] scorerCodes;
    private final double[] scorerWeights;
    private final double buyThreshold;
    private final BacktestConfig backtest;

    // mode 数字→名称映射
    private static final String[] MODES = {"", "backtest", "analysis", "import", "stats"};

    private RunConfig(Properties p) {
        int modeNum = parseInt(p, "mode", 1);
        this.mode = (modeNum >= 1 && modeNum < MODES.length) ? MODES[modeNum] : "backtest";

        // 策略组装：从配置文件读取选股链和打分链
        this.selectorCodes = parseIntArray(p.getProperty("backtest.selector.codes", "1"));
        this.scorerCodes = parseIntArray(p.getProperty("backtest.scorer.codes", "501,601"));
        this.scorerWeights = parseDoubleArray(p.getProperty("backtest.scorer.weights", "1.0"));
        this.buyThreshold = parseDouble(p, "backtest.buyThreshold", 60);
        StringBuilder sb = new StringBuilder();
        for (int c : selectorCodes) sb.append(c).append(",");
        sb.setLength(sb.length() - 1);
        this.strategyName = "S" + sb + "_C" + String.join(",", p.getProperty("backtest.scorer.codes", ""));
        this.dateFrom = parseDate(p.getProperty("backtest.date.from"));
        this.dateTo = parseDate(p.getProperty("backtest.date.to"));
        this.primaryCode = p.getProperty("analysis.primaryCode", "");
        this.compareCodes = p.getProperty("analysis.compareCodes", "");
        this.importYear = p.getProperty("import.year", "all");

        this.backtest = BacktestConfig.builder()
                .initialCapital(parseDouble(p, "backtest.capital", 100_000))
                .maxPositions(parseInt(p, "backtest.maxPositions", 5))
                .cashRatio(parseDouble(p, "backtest.cashRatio", 1.0))
                .commissionRate(parseDouble(p, "backtest.commissionRate", 0.00025))
                .minCommission(parseDouble(p, "backtest.minCommission", 5.0))
                .stampTaxRate(parseDouble(p, "backtest.stampTaxRate", 0.001))
                .stopLossPct(parseDouble(p, "backtest.stopLossPct", -1))
                .takeProfitPct(parseDouble(p, "backtest.takeProfitPct", -1))
                .maxHoldingDays(parseInt(p, "backtest.maxHoldingDays", -1))
                .forceCloseAtEnd(parseBool(p, "backtest.forceCloseAtEnd", true))
                .replaceLowerScored(parseBool(p, "backtest.replaceLowerScored", false))
                .allowAddPosition(parseBool(p, "backtest.allowAddPosition", false))
                .riskFreeRate(parseDouble(p, "backtest.riskFreeRate", 0.02))
                .build();
    }

    /** 从输入流加载 */
    public static RunConfig load(InputStream in) {
        Properties p = new Properties();
        try (java.io.Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            p.load(reader);
        } catch (IOException e) {
            throw new RuntimeException("读取运行配置失败", e);
        }
        return new RunConfig(p);
    }

    // === Getters ===
    public String mode() { return mode; }
    public String strategyName() { return strategyName; }
    public LocalDate dateFrom() { return dateFrom; }
    public LocalDate dateTo() { return dateTo; }
    public String primaryCode() { return primaryCode; }
    public String compareCodes() { return compareCodes; }
    public String importYear() { return importYear; }
    public int[] selectorCodes() { return selectorCodes; }
    public int[] scorerCodes() { return scorerCodes; }
    public double[] scorerWeights() { return scorerWeights; }
    public double buyThreshold() { return buyThreshold; }
    public BacktestConfig backtest() { return backtest; }

    // === Helper parsers ===
    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); }
        catch (Exception e) { return null; }
    }

    private static double parseDouble(Properties p, String key, double def) {
        try { return Double.parseDouble(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static int parseInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static int[] parseIntArray(String s) {
        return Arrays.stream(s.split(",")).map(String::trim)
                .mapToInt(Integer::parseInt).toArray();
    }

    private static double[] parseDoubleArray(String s) {
        return Arrays.stream(s.split(",")).map(String::trim)
                .mapToDouble(Double::parseDouble).toArray();
    }

    private static boolean parseBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }
}
