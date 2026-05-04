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
 * 单日爆量选择器。
 * <p>
 * 保留当日成交量 &gt; 前日成交量 &times; multiplier 的股票。
 * 爆量通常伴随主力资金进出，是重要的盘面信号。
 */
public class VolumeSpikeSelector implements StockSelector {

    private final double multiplier;
    private final int code;

    public VolumeSpikeSelector(double multiplier, int code) {
        if (multiplier <= 1) throw new IllegalArgumentException("multiplier 必须 > 1");
        this.multiplier = multiplier;
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
                WITH spike AS (
                  SELECT k.stock_code, k.volume,
                    (SELECT prev.volume FROM daily_kline prev
                     WHERE prev.stock_code = k.stock_code AND prev.date < k.date
                     ORDER BY prev.date DESC LIMIT 1) AS prev_vol
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM spike
                WHERE prev_vol IS NOT NULL AND prev_vol > 0
                  AND volume > prev_vol * ?
                """.formatted(ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, multiplier);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("爆量筛选查询失败", e);
        }
        return result;
    }
}
