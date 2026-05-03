package com.stock.db;

import com.stock.model.KLine;
import com.stock.model.MinuteData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据仓库。从 SQLite 数据库读取日K线和分时数据。
 */
public class KLineRepository {

    private final DatabaseManager db;

    public KLineRepository() {
        this.db = DatabaseManager.getInstance();
    }

    // ======================== 日K线 ========================

    public List<KLine> findByStockCode(String stockCode) {
        List<KLine> result = new ArrayList<>();
        String sql = "SELECT date, open, high, low, close, volume, amount " +
                     "FROM daily_kline WHERE stock_code = ? ORDER BY date ASC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToKLine(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询K线数据失败: " + stockCode, e);
        }
        return result;
    }

    public List<String> getAllStockCodes() {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT DISTINCT stock_code FROM daily_kline ORDER BY stock_code";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                codes.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询股票列表失败", e);
        }
        return codes;
    }

    public int getStockCount() {
        String sql = "SELECT COUNT(DISTINCT stock_code) FROM daily_kline";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.getInt(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    public long getTotalRows() {
        String sql = "SELECT COUNT(*) FROM daily_kline";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.getLong(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    public boolean exists(String stockCode) {
        String sql = "SELECT 1 FROM daily_kline WHERE stock_code = ? LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private KLine rowToKLine(ResultSet rs) throws SQLException {
        return KLine.builder()
                .date(LocalDate.parse(rs.getString("date")))
                .open(rs.getDouble("open"))
                .high(rs.getDouble("high"))
                .low(rs.getDouble("low"))
                .close(rs.getDouble("close"))
                .volume(rs.getDouble("volume"))
                .amount(rs.getDouble("amount"))
                .build();
    }

    // ======================== 分时数据 ========================

    public List<MinuteData> findMinuteByStockCode(String stockCode) {
        List<MinuteData> result = new ArrayList<>();
        String sql = "SELECT date, time, open, high, low, close, volume, amount " +
                     "FROM minute_kline WHERE stock_code = ? ORDER BY date ASC, time ASC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMinuteData(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询分时数据失败: " + stockCode, e);
        }
        return result;
    }

    public boolean existsMinute(String stockCode) {
        String sql = "SELECT 1 FROM minute_kline WHERE stock_code = ? LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public long getMinuteTotalRows() {
        String sql = "SELECT COUNT(*) FROM minute_kline";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.getLong(1);
        } catch (SQLException e) {
            return 0;
        }
    }

    private MinuteData rowToMinuteData(ResultSet rs) throws SQLException {
        return MinuteData.builder()
                .date(LocalDate.parse(rs.getString("date")))
                .time(rs.getString("time"))
                .open(rs.getDouble("open"))
                .high(rs.getDouble("high"))
                .low(rs.getDouble("low"))
                .close(rs.getDouble("close"))
                .volume(rs.getDouble("volume"))
                .amount(rs.getDouble("amount"))
                .build();
    }
}
