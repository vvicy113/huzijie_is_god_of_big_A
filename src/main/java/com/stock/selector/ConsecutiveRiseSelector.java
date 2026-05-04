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
 * 连阳选择器。
 * <p>
 * 保留连续 N 日收盘价 &gt; 前日收盘价的股票。
 * 通过 SQL 窗口函数 COUNT 连续上涨天数。
 */
public class ConsecutiveRiseSelector implements StockSelector {

    private final int nDays;
    private final int code;

    public ConsecutiveRiseSelector(int nDays, int code) {
        if (nDays < 2) throw new IllegalArgumentException("nDays 必须 >= 2");
        this.nDays = nDays;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        LocalDate lookbackStart = date.minusDays(nDays * 2);
        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT stock_code FROM (
                  SELECT stock_code, date, close,
                    SUM(CASE WHEN close > LAG(close) OVER (
                      PARTITION BY stock_code ORDER BY date) THEN 0 ELSE 1 END
                    ) OVER (
                      PARTITION BY stock_code ORDER BY date
                      ROWS BETWEEN %d PRECEDING AND 1 PRECEDING
                    ) AS fall_count
                  FROM daily_kline
                  WHERE stock_code IN (%s)
                    AND date >= ? AND date <= ?
                )
                WHERE date = ? AND fall_count = 0
                """.formatted(nDays - 1, placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (String c : candidates) ps.setString(idx++, c);
            ps.setString(idx++, lookbackStart.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("连阳筛选查询失败", e);
        }
        return result;
    }
}
