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
 * 缩量选择器。
 * <p>
 * 保留当日成交量 &le; N日均量 &times; 倍数 的股票（成交量缩减）。
 * 常用于寻找地量或筑底信号。
 */
public class ShrinkVolumeSelector implements StockSelector {

    private final int nDays;
    private final double multiplier;
    private final int code;

    public ShrinkVolumeSelector(int nDays, double multiplier, int code) {
        if (nDays < 1) throw new IllegalArgumentException("nDays 必须 >= 1");
        if (multiplier < 0) throw new IllegalArgumentException("multiplier 必须 >= 0");
        this.nDays = nDays;
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

        LocalDate startDate = date.minusDays(nDays * 7 / 5);
        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT k.stock_code FROM daily_kline k
                WHERE k.date = ?
                  AND k.stock_code IN (%s)
                  AND k.volume <= (
                    SELECT AVG(k2.volume) * ? FROM daily_kline k2
                    WHERE k2.stock_code = k.stock_code
                      AND k2.date >= ? AND k2.date < ?
                  )
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, multiplier);
            ps.setString(idx++, startDate.toString());
            ps.setString(idx++, date.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("缩量筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
