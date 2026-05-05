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
 * N日动量选择器。
 * <p>
 * 保留 N 日累计涨跌幅在 [minPct, maxPct]% 之间的股票。
 * 动量 = (今日收盘 − N日前收盘) / N日前收盘 × 100。
 */
public class MomentumSelector implements StockSelector {

    private final int nDays;
    private final double minPct;
    private final double maxPct;
    private final int code;

    public MomentumSelector(int nDays, double minPct, double maxPct, int code) {
        if (nDays < 1) throw new IllegalArgumentException("nDays 必须 >= 1");
        if (minPct > maxPct) throw new IllegalArgumentException("minPct 必须 <= maxPct");
        this.nDays = nDays;
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

        LocalDate nDaysAgo = date.minusDays(nDays * 7 / 5);
        String ph = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH mom AS (
                  SELECT k.stock_code, k.close AS today_close,
                    (SELECT prev.close FROM daily_kline prev
                     WHERE prev.stock_code = k.stock_code AND prev.date < k.date
                     ORDER BY prev.date DESC LIMIT ?) AS n_ago_close
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM mom
                WHERE n_ago_close IS NOT NULL AND n_ago_close > 0
                  AND (today_close - n_ago_close) / n_ago_close * 100 BETWEEN ? AND ?
                """.formatted(ph);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, nDays);
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minPct);
            ps.setDouble(idx++, maxPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("动量筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
