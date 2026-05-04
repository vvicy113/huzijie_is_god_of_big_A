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
 * 跳空高开选择器。
 * <p>
 * 保留当日开盘价 &gt; 前日收盘价 × (1 + minPct/100) 的股票。
 * 即开盘即跳空高开超过指定幅度的股票。
 */
public class GapUpSelector implements StockSelector {

    private final double minPct;
    private final int code;

    public GapUpSelector(double minPct, int code) {
        if (minPct < 0) throw new IllegalArgumentException("minPct 必须 >= 0");
        this.minPct = minPct;
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
                WITH gap_data AS (
                  SELECT k.stock_code, k.open,
                    (SELECT prev.close FROM daily_kline prev
                     WHERE prev.stock_code = k.stock_code AND prev.date < k.date
                     ORDER BY prev.date DESC LIMIT 1) AS prev_close
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM gap_data
                WHERE prev_close IS NOT NULL AND prev_close > 0
                  AND open > prev_close * (1 + ? / 100.0)
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("跳空高开筛选查询失败", e);
        }
        return result;
    }
}
