package com.stock.selector;

import com.stock.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 涨跌幅选择器。
 * <p>
 * 保留当日涨跌幅在 [minPercent, maxPercent] 区间内的股票。
 * 涨跌幅 = (今日收盘 − 昨日收盘) / 昨日收盘 × 100。
 */
public class PriceChangeSelector implements StockSelector {

    private final double minPercent;
    private final double maxPercent;
    private final int code;

    public PriceChangeSelector(double minPercent, double maxPercent, int code) {
        if (minPercent > maxPercent)
            throw new IllegalArgumentException("minPercent 必须 <= maxPercent");
        this.minPercent = minPercent;
        this.maxPercent = maxPercent;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH price_data AS (
                  SELECT k.stock_code, k.close AS today_close,
                    (SELECT prev.close FROM daily_kline prev
                     WHERE prev.stock_code = k.stock_code AND prev.date < k.date
                     ORDER BY prev.date DESC LIMIT 1) AS prev_close
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM price_data
                WHERE prev_close IS NOT NULL AND prev_close > 0
                  AND (today_close - prev_close) / prev_close * 100 BETWEEN ? AND ?
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minPercent);
            ps.setDouble(idx++, maxPercent);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("涨跌幅筛选查询失败", e);
        }
        return result;
    }
}
