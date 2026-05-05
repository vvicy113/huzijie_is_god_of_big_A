package com.stock.comparator;

import com.stock.constants.ComparatorConstants;
import com.stock.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动量打分器——N 日累计涨幅越高分越高。
 */
public class MomentumScorer implements StockComparator {

    private final int nDays;
    private final int code;

    public MomentumScorer(int nDays, int code) {
        if (nDays < 1) throw new IllegalArgumentException("nDays 必须 >= 1");
        this.nDays = nDays;
        this.code = code;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty()) return Collections.emptyMap();

        String ph = codes.stream().map(c -> "?").collect(Collectors.joining(","));

        // 取 N 个交易日前的收盘价
        String sql = ("WITH hist AS ("
            + " SELECT stock_code, close,"
            + " ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY date DESC) AS rn"
            + " FROM daily_kline"
            + " WHERE stock_code IN (%s) AND date <= ?"
            + ") SELECT stock_code, close FROM hist WHERE rn = %%d"
        ).formatted(ph);

        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            // 今日收盘
            String sqlToday = String.format(sql, 1);
            Map<String, Double> todayClose = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlToday)) {
                int idx = 1;
                for (String c : codes) ps.setString(idx++, c);
                ps.setString(idx, date.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) todayClose.put(rs.getString("stock_code"), rs.getDouble("close"));
                }
            }

            // N日前收盘
            String sqlPrev = String.format(sql, nDays + 1);
            try (PreparedStatement ps = conn.prepareStatement(sqlPrev)) {
                int idx = 1;
                for (String c : codes) ps.setString(idx++, c);
                ps.setString(idx, date.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString("stock_code");
                        double prevClose = rs.getDouble("close");
                        Double today = todayClose.get(code);
                        if (today != null && prevClose > 0) {
                            double momentum = (today - prevClose) / prevClose * 100;
                            result.put(code, Math.max(0, momentum));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("动量打分失败", e);
        }
        return result;
    }

    @Override
    public String getDesc() { return ComparatorConstants.getDesc(code); }
}
