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
 * 上涨趋势确认选择器。
 * <p>
 * 保留 N 日中上涨天数占比 &ge; minRatio 的股票（上涨 = 当日收盘 &gt; 前日收盘）。
 */
public class UpTrendRatioSelector implements StockSelector {

    private final int nDays;
    private final double minRatio;
    private final int code;

    public UpTrendRatioSelector(int nDays, double minRatio, int code) {
        if (nDays < 2) throw new IllegalArgumentException("nDays 必须 >= 2");
        if (minRatio <= 0 || minRatio > 1)
            throw new IllegalArgumentException("minRatio 必须在 (0, 1]");
        this.nDays = nDays;
        this.minRatio = minRatio;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        LocalDate start = date.minusDays(nDays * 7 / 5);
        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT stock_code FROM (
                  SELECT stock_code,
                    SUM(CASE WHEN close > LAG(close) OVER (
                      PARTITION BY stock_code ORDER BY date) THEN 1 ELSE 0 END
                    ) OVER (PARTITION BY stock_code ORDER BY date
                      ROWS BETWEEN %d PRECEDING AND CURRENT ROW) * 1.0 / %d AS up_ratio
                  FROM daily_kline
                  WHERE stock_code IN (%s) AND date >= ? AND date <= ?
                )
                WHERE date = ? AND up_ratio >= ?
                """.formatted(nDays - 1, nDays, ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            for (String c : candidates) ps.setString(idx++, c);
            ps.setString(idx++, start.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            ps.setDouble(idx++, minRatio);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("上涨趋势筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
