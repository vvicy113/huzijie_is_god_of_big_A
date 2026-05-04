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
 * 布林下轨选择器。
 * <p>
 * 保留收盘价 ≤ MA − K×σ 的股票（触及或跌破布林带下轨）。
 * 常用于均值回归策略——超卖后可能反弹。
 */
public class BollingerLowerSelector implements StockSelector {

    private final int nDays;
    private final double k;
    private final int code;

    public BollingerLowerSelector(int nDays, double k, int code) {
        if (nDays < 5) throw new IllegalArgumentException("nDays 必须 >= 5");
        if (k <= 0) throw new IllegalArgumentException("k 必须 > 0");
        this.nDays = nDays;
        this.k = k;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        LocalDate start = date.minusDays(nDays - 1);
        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH stats AS (
                  SELECT k.stock_code, k.close,
                    (SELECT AVG(m.close) FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS ma,
                    (SELECT AVG(m.close * m.close) - AVG(m.close) * AVG(m.close)
                     FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS var
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM stats
                WHERE ma IS NOT NULL AND var IS NOT NULL AND var > 0
                  AND close <= ma - ? * SQRT(var)
                """.formatted(ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, start.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, start.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, k);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("布林下轨筛选查询失败", e);
        }
        return result;
    }
}
