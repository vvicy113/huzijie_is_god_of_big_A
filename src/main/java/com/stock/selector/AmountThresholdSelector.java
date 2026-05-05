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
 * 成交额阈值选择器。
 * <p>
 * 保留当日成交额 &ge; minAmount 的股票。用于过滤流动性过低的标的。
 */
public class AmountThresholdSelector implements StockSelector {

    private final double minAmount;
    private final int code;

    /**
     * @param minAmount 最小成交额（元），如 100_000_000 表示 1 亿
     */
    public AmountThresholdSelector(double minAmount, int code) {
        if (minAmount < 0) throw new IllegalArgumentException("minAmount 必须 >= 0");
        this.minAmount = minAmount;
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        if (candidates.isEmpty()) return candidates;

        String placeholders = candidates.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = """
                SELECT stock_code FROM daily_kline
                WHERE date = ? AND stock_code IN (%s) AND amount >= ?
                """.formatted(placeholders);

        List<String> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : candidates) ps.setString(idx++, c);
            ps.setDouble(idx++, minAmount);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("成交额筛选查询失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return SelectorConstants.getDesc(code); }
}
