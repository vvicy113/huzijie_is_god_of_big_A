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
 * K线实体占比选择器。
 * <p>
 * 保留实体长度 / 振幅 &ge; minRatio 的股票。
 * 实体 = |close − open|，振幅 = high − low。
 * 比率高表示实体饱满、方向明确；比率低表示十字星/小实体，方向不明确。
 */
public class BodyRatioSelector implements StockSelector {

    private final double minRatio;
    private final int code;

    public BodyRatioSelector(double minRatio, int code) {
        if (minRatio < 0 || minRatio > 1)
            throw new IllegalArgumentException("minRatio 必须在 [0, 1]");
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
                  AND (high - low) > 0
                  AND ABS(close - open) / (high - low) >= ?
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
            throw new RuntimeException("实体占比筛选查询失败", e);
        }
        return result;
    }
}
