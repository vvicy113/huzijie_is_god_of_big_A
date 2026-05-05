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
 * 布林收口选择器。
 * <p>
 * 保留布林带宽度 (上轨−下轨)/MA &le; maxPct% 的股票。布林收口通常预示着即将变盘突破。
 * 带宽 = (2 × K × σ) / MA。
 */
public class BollingerSqueezeSelector implements StockSelector {

    private final int nDays;
    private final double k;
    private final double maxPct;
    private final int code;

    public BollingerSqueezeSelector(int nDays, double k, double maxPct, int code) {
        if (nDays < 5) throw new IllegalArgumentException("nDays 必须 >= 5");
        if (k <= 0) throw new IllegalArgumentException("k 必须 > 0");
        if (maxPct <= 0) throw new IllegalArgumentException("maxPct 必须 > 0");
        this.nDays = nDays;
        this.k = k;
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

        LocalDate start = date.minusDays(nDays - 1);
        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH stats AS (
                  SELECT k.stock_code,
                    (SELECT AVG(m.close) FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS ma,
                    (SELECT AVG(m.close * m.close) - AVG(m.close) * AVG(m.close)
                     FROM daily_kline m
                     WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS var
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM stats
                WHERE ma IS NOT NULL AND var IS NOT NULL AND var > 0 AND ma > 0
                  AND 2 * ? * SQRT(var) / ma * 100 <= ?
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
            ps.setDouble(idx++, maxPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("布林收口筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
