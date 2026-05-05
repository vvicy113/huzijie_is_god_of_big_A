package com.stock.selector;

import com.stock.constants.SelectorConstants;
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
 * 相对位置选择器。
 * <p>
 * 保留收盘价在 N 日价格区间中的相对位置处于 [lowPct, highPct]% 的股票。
 * 位置 = (close − N日最低) / (N日最高 − N日最低) × 100。
 * 0% = N日最低点，100% = N日最高点。
 */
public class PositionRankSelector implements StockSelector {

    private final int nDays;
    private final double lowPct;
    private final double highPct;
    private final int code;

    public PositionRankSelector(int nDays, double lowPct, double highPct, int code) {
        if (nDays < 2) throw new IllegalArgumentException("nDays 必须 >= 2");
        if (lowPct < 0 || highPct > 100 || lowPct >= highPct)
            throw new IllegalArgumentException("区间无效");
        this.nDays = nDays;
        this.lowPct = lowPct;
        this.highPct = highPct;
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
                WITH pos AS (
                  SELECT k.stock_code, k.close,
                    (SELECT MIN(m.low) FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS n_low,
                    (SELECT MAX(m.high) FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS n_high
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM pos
                WHERE n_high > n_low
                  AND (close - n_low) / (n_high - n_low) * 100 BETWEEN ? AND ?
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
            ps.setDouble(idx++, lowPct);
            ps.setDouble(idx++, highPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("相对位置筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
