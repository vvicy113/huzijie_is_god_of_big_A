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
 * 量比打分器——当日成交量 / N日均量，量比越高分越高（放量活跃）。
 */
public class VolumeRatioScorer implements StockComparator {

    private final int nDays;
    private final int code;

    public VolumeRatioScorer(int nDays, int code) {
        if (nDays < 1) throw new IllegalArgumentException("nDays 必须 >= 1");
        this.nDays = nDays;
        this.code = code;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty()) return Collections.emptyMap();

        LocalDate start = date.minusDays(nDays * 7 / 5);
        String ph = codes.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = ("SELECT k.stock_code, k.volume / ("
            + " SELECT AVG(v.volume) FROM daily_kline v"
            + " WHERE v.stock_code = k.stock_code AND v.date >= ? AND v.date < ?"
            + ") AS ratio FROM daily_kline k"
            + " WHERE k.date = ? AND k.stock_code IN (%s)"
        ).formatted(ph);

        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, start.toString());
            ps.setString(idx++, date.toString());
            ps.setString(idx++, date.toString());
            for (String c : codes) ps.setString(idx++, c);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double ratio = rs.getDouble("ratio");
                    result.put(rs.getString("stock_code"), Math.max(0, ratio));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("量比打分失败", e);
        }
        return result;
    }
}
