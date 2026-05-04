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
 * 均线乖离打分器——收盘价越贴近均线分数越高。
 * score = max(0, 100 − |close−MA|/MA × 100)，即乖离率越小分越高。
 */
public class MaDistanceScorer implements StockComparator {

    private final int nDays;
    private final int code;

    public MaDistanceScorer(int nDays, int code) {
        if (nDays < 2) throw new IllegalArgumentException("nDays 必须 >= 2");
        this.nDays = nDays;
        this.code = code;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty()) return Collections.emptyMap();

        LocalDate start = date.minusDays(nDays - 1);
        String ph = codes.stream().map(c -> "?").collect(Collectors.joining(","));

        String sql = ("WITH ma AS ("
            + " SELECT k.stock_code, k.close,"
            + " (SELECT AVG(m.close) FROM daily_kline m"
            + "  WHERE m.stock_code = k.stock_code AND m.date >= ? AND m.date <= ?) AS ma_val"
            + " FROM daily_kline k"
            + " WHERE k.date = ? AND k.stock_code IN (%s)"
            + ") SELECT stock_code, MAX(0, 100 - ABS(close - ma_val) / ma_val * 100) AS s"
            + " FROM ma WHERE ma_val IS NOT NULL AND ma_val > 0"
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
                while (rs.next()) result.put(rs.getString("stock_code"), rs.getDouble("s"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("均线乖离打分失败", e);
        }
        return result;
    }
}
