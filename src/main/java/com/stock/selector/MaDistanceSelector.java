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
 * 均线乖离率选择器。
 * <p>
 * 保留收盘价与 N 日均线的乖离率绝对值 &le; maxPct% 的股票。
 * 乖离率 = |close − MA| / MA × 100。数值越小表示股价贴近均线。
 */
public class MaDistanceSelector implements StockSelector {

    private final int nDays;
    private final double maxPct;
    private final int code;

    public MaDistanceSelector(int nDays, double maxPct, int code) {
        if (nDays < 2) throw new IllegalArgumentException("nDays 必须 >= 2");
        if (maxPct <= 0) throw new IllegalArgumentException("maxPct 必须 > 0");
        this.nDays = nDays;
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

        LocalDate startDate = date.minusDays(nDays - 1);
        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                WITH ma_data AS (
                  SELECT k.stock_code, k.close,
                    (SELECT AVG(m.close) FROM daily_kline m
                     WHERE m.stock_code = k.stock_code
                       AND m.date >= ? AND m.date <= ?) AS ma
                  FROM daily_kline k
                  WHERE k.date = ? AND k.stock_code IN (%s)
                )
                SELECT stock_code FROM ma_data
                WHERE ma IS NOT NULL AND ma > 0
                  AND ABS(close - ma) / ma * 100 <= ?
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, startDate.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, maxPct);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("均线乖离率筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
