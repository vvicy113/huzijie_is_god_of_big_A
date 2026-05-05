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
 * 均线金叉选择器。
 * <p>
 * 保留当日短期均线上穿长期均线的股票（金叉发生在今天）。
 * 金叉条件：昨日 shortMA &le; longMA 且 今日 shortMA &gt; longMA。
 */
public class MaGoldenCrossSelector implements StockSelector {

    private final int shortWindow;
    private final int longWindow;
    private final int code;

    public MaGoldenCrossSelector(int shortWindow, int longWindow, int code) {
        if (shortWindow >= longWindow)
            throw new IllegalArgumentException("shortWindow 必须 < longWindow");
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        int maxWin = Math.max(shortWindow, longWindow) + 1;
        LocalDate start = date.minusDays(maxWin * 7 / 5);
        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH ma AS (
                  SELECT stock_code, date,
                    AVG(close) OVER (PARTITION BY stock_code ORDER BY date
                      ROWS BETWEEN ? - 1 PRECEDING AND CURRENT ROW) AS short_ma,
                    AVG(close) OVER (PARTITION BY stock_code ORDER BY date
                      ROWS BETWEEN ? - 1 PRECEDING AND CURRENT ROW) AS long_ma
                  FROM daily_kline
                  WHERE stock_code IN (%s) AND date >= ? AND date <= ?
                ),
                today AS (
                  SELECT stock_code, short_ma, long_ma FROM ma WHERE date = ?
                ),
                yesterday AS (
                  SELECT stock_code, short_ma, long_ma FROM ma WHERE date < ?
                  AND stock_code IN (SELECT stock_code FROM today)
                  GROUP BY stock_code HAVING date = MAX(date)
                )
                SELECT t.stock_code FROM today t JOIN yesterday y ON t.stock_code = y.stock_code
                WHERE y.short_ma <= y.long_ma AND t.short_ma > t.long_ma
                """.formatted(ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, shortWindow);
            ps.setInt(idx++, longWindow);
            for (String c : candidates) ps.setString(idx++, c);
            ps.setString(idx++, start.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("均线金叉筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
