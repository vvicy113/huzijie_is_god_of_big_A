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
 * 长下影线选择器（锤子线信号）。
 * <p>
 * 保留下影线长度 &ge; 实体长度 &times; minRatio 的股票。
 * 实体 = |close − open|，下影线 = min(open, close) − low。
 * 长下影线通常表示下方支撑强劲，是潜在的看涨反转信号。
 */
public class LowerShadowSelector implements StockSelector {

    private final double minRatio;
    private final int code;

    public LowerShadowSelector(double minRatio, int code) {
        if (minRatio < 1) throw new IllegalArgumentException("minRatio 必须 >= 1");
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

        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT stock_code FROM daily_kline
                WHERE date = ? AND stock_code IN (%s)
                  AND ABS(close - open) > 0
                  AND (MIN(open, close) - low) >= ABS(close - open) * ?
                """.formatted(ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minRatio);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("长下影线筛选查询失败", e);
        }
        return result;
    }
}
