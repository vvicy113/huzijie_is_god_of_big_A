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
 * 均线多头排列选择器。
 * <p>
 * 保留短期均线 &gt; 长期均线的股票，表示处于上涨趋势中。
 * 均线 = 过去 N 个交易日的收盘价简单算术平均（使用日历日近似）。
 */
public class MaAlignmentSelector implements StockSelector {

    private final int shortWindow;
    private final int longWindow;
    private final int code;

    public MaAlignmentSelector(int shortWindow, int longWindow, int code) {
        if (shortWindow >= longWindow)
            throw new IllegalArgumentException("shortWindow 必须 < longWindow");
        if (shortWindow < 1)
            throw new IllegalArgumentException("shortWindow 必须 >= 1");
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

        LocalDate shortStart = date.minusDays(shortWindow - 1);
        LocalDate longStart = date.minusDays(longWindow - 1);
        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH ma_data AS (
                  SELECT k.stock_code,
                    (SELECT AVG(s.close) FROM daily_kline s
                     WHERE s.stock_code = k.stock_code
                       AND s.date >= ? AND s.date <= ?) AS short_ma,
                    (SELECT AVG(l.close) FROM daily_kline l
                     WHERE l.stock_code = k.stock_code
                       AND l.date >= ? AND l.date <= ?) AS long_ma
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM ma_data
                WHERE short_ma IS NOT NULL AND long_ma IS NOT NULL
                  AND short_ma > long_ma
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, shortStart.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, longStart.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("均线多头筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
