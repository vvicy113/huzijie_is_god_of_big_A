package com.stock.comparator;

import com.stock.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 趋势强度打分器。
 * <p>
 * 趋势强度 = N日涨幅 / N日波动率。
 * 分子越大（涨得多）、分母越小（涨得稳），分数越高。
 * 相当于在衡量"涨得有多稳"——同样涨 10%，每天稳步涨的比暴涨暴跌的分数高。
 */
public class TrendStrengthScorer implements StockComparator {

    private final int nDays;
    private final int code;

    public TrendStrengthScorer(int nDays, int code) {
        if (nDays < 3) throw new IllegalArgumentException("nDays 必须 >= 3");
        this.nDays = nDays;
        this.code = code;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getDesc() { return com.stock.constants.ComparatorConstants.getDesc(code); }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty()) return Collections.emptyMap();

        String ph = codes.stream().map(c -> "?").collect(Collectors.joining(","));

        // 取每只股票最近 nDays 条的收盘价
        String sql = ("SELECT stock_code, close FROM ("
            + " SELECT stock_code, close,"
            + " ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY date DESC) AS rn"
            + " FROM daily_kline WHERE stock_code IN (%s) AND date <= ?"
            + ") WHERE rn <= %%d ORDER BY stock_code, rn DESC"
        ).formatted(ph);

        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            // 取 nDays 条收盘价，窗口函数按 rn 倒序取最近N条
            String q = String.format(sql, nDays);
            Map<String, List<Double>> priceMap = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                int idx = 1;
                for (String c : codes) ps.setString(idx++, c);
                ps.setString(idx, date.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        priceMap.computeIfAbsent(rs.getString("stock_code"), k -> new ArrayList<>())
                                .add(rs.getDouble("close"));
                    }
                }
            }

            // 逐只计算趋势强度
            for (String code : codes) {
                List<Double> prices = priceMap.get(code);
                if (prices == null || prices.size() < Math.min(3, nDays)) continue;

                int n = prices.size();
                double first = prices.get(0);
                double last = prices.get(n - 1);
                if (first <= 0) continue;

                // N日涨幅
                double totalReturn = (last - first) / first * 100;
                if (totalReturn <= 0) continue; // 下跌趋势不参与打分

                // 日波动率（标准差）
                double sum = 0;
                for (int i = 1; i < n; i++) {
                    double dailyRet = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1) * 100;
                    sum += dailyRet;
                }
                double mean = sum / (n - 1);
                double variance = 0;
                for (int i = 1; i < n; i++) {
                    double dailyRet = (prices.get(i) - prices.get(i - 1)) / prices.get(i - 1) * 100;
                    variance += (dailyRet - mean) * (dailyRet - mean);
                }
                double stdDev = Math.sqrt(variance / (n - 1));

                // 趋势强度 = 涨幅 / 波动率（类似夏普比率）
                if (stdDev > 0) {
                    result.put(code, totalReturn / (1 + stdDev));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("趋势强度打分失败", e);
        }
        return result;
    }
}
