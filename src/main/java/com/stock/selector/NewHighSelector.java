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
 * N日新高选择器。
 * <p>
 * 保留当日收盘价等于 N 日内最高价的股票（创 N 日新高）。
 */
public class NewHighSelector implements StockSelector {

    private final int nDays;
    private final int code;

    public NewHighSelector(int nDays, int code) {
        if (nDays < 1) throw new IllegalArgumentException("nDays 必须 >= 1");
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

        LocalDate startDate = date.minusDays(nDays - 1);
        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT k.stock_code FROM daily_kline k
                WHERE k.date = ?
                  AND k.stock_code IN (%s)
                  AND k.close >= (
                    SELECT MAX(h.high) FROM daily_kline h
                    WHERE h.stock_code = k.stock_code
                      AND h.date >= ? AND h.date < ?
                  )
                  AND k.close = (
                    SELECT MAX(h2.high) FROM daily_kline h2
                    WHERE h2.stock_code = k.stock_code
                      AND h2.date >= ? AND h2.date <= ?
                  )
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setString(idx++, startDate.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, startDate.toString());
            ps.setString(idx++, date.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("新高筛选查询失败", e);
        }
        return result;
    }
}
