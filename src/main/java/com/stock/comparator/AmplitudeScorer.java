package com.stock.comparator;

import com.stock.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 振幅打分器——日振幅 (high−low)/prev_close × 100 越高分越高（活跃度）。
 */
public class AmplitudeScorer implements StockComparator {

    private final int code;

    public AmplitudeScorer(int code) {
        this.code = code;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty()) return Collections.emptyMap();

        String ph = codes.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = ("WITH amp AS ("
            + " SELECT k.stock_code, k.high, k.low,"
            + " (SELECT prev.close FROM daily_kline prev"
            + "  WHERE prev.stock_code = k.stock_code AND prev.date < k.date"
            + "  ORDER BY prev.date DESC LIMIT 1) AS prev_close"
            + " FROM daily_kline k"
            + " WHERE k.date = ? AND k.stock_code IN (%s)"
            + ") SELECT stock_code, (high - low) / prev_close * 100 AS s"
            + " FROM amp WHERE prev_close IS NOT NULL AND prev_close > 0"
        ).formatted(ph);

        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, date.toString());
            for (String c : codes) ps.setString(idx++, c);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getString("stock_code"), rs.getDouble("s"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("振幅打分失败", e);
        }
        return result;
    }
}
