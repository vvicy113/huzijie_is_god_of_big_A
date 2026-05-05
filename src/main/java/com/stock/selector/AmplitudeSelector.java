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
 * 日振幅选择器。
 * <p>
 * 保留当日振幅 (最高价−最低价)/前日收盘价 在 [minPct, maxPct] 区间内的股票。
 */
public class AmplitudeSelector implements StockSelector {

    private final double minPct;
    private final double maxPct;
    private final int code;

    public AmplitudeSelector(double minPct, double maxPct, int code) {
        if (minPct < 0) throw new IllegalArgumentException("minPct 必须 >= 0");
        if (maxPct < minPct) throw new IllegalArgumentException("maxPct 必须 >= minPct");
        this.minPct = minPct;
        this.maxPct = maxPct;
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
                WITH amp_data AS (
                  SELECT k.stock_code, k.high, k.low,
                    (SELECT prev.close FROM daily_kline prev
                     WHERE prev.stock_code = k.stock_code AND prev.date < k.date
                     ORDER BY prev.date DESC LIMIT 1) AS prev_close
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM amp_data
                WHERE prev_close IS NOT NULL AND prev_close > 0
                  AND (high - low) / prev_close * 100 BETWEEN ? AND ?
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minPct);
            ps.setDouble(idx++, maxPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("振幅筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
