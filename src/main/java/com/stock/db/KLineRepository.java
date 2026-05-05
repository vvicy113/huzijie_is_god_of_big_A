package com.stock.db;

import com.stock.model.KLine;
import com.stock.model.StockInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 日K线数据仓库。优先从缓存读取，缓存未命中时查 SQLite 并回写缓存。
 */
public class KLineRepository {

    private static volatile KLineCache globalCache = new NoopKLineCache();

    private final DatabaseManager db;
    private final KLineCache cache;

    /** 设置全局缓存（通常启动时由 App 调用一次） */
    public static void setGlobalCache(KLineCache cache) {
        globalCache = cache != null ? cache : new NoopKLineCache();
    }

    public KLineRepository() {
        this.db = DatabaseManager.getInstance();
        this.cache = globalCache;
    }

    public List<KLine> findByStockCode(String stockCode) {
        return findByStockCode(stockCode, null, null);
    }

    public List<KLine> findByStockCode(String stockCode, LocalDate from, LocalDate to) {
        // 1. 先查缓存
        if (cache.isAvailable()) {
            List<KLine> all = cache.get(stockCode);
            if (all != null) return filterByDate(all, from, to);
        }

        // 2. 缓存未命中，查SQLite
        List<KLine> all = queryDb(stockCode);

        // 3. 回写缓存
        if (cache.isAvailable() && !all.isEmpty()) {
            cache.put(stockCode, all);
        }

        return filterByDate(all, from, to);
    }

    private List<KLine> queryDb(String stockCode) {
        List<KLine> result = new ArrayList<>();
        String sql = "SELECT date, open, high, low, close, volume, amount " +
                     "FROM daily_kline WHERE stock_code = ? ORDER BY date ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(rowToKLine(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询K线数据失败: " + stockCode, e);
        }
        return result;
    }

    /** 在内存中过滤日期范围 */
    private List<KLine> filterByDate(List<KLine> all, LocalDate from, LocalDate to) {
        if (from == null && to == null) return all;
        List<KLine> result = new ArrayList<>();
        for (KLine k : all) {
            if (from != null && k.getDate().isBefore(from)) continue;
            if (to != null && k.getDate().isAfter(to)) continue;
            result.add(k);
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

    /** 查询股票元信息 */
    public StockInfo findStockInfo(String stockCode) {
        String sql = "SELECT stock_code, name, market, industry, area FROM stock_info WHERE stock_code = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stockCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return StockInfo.builder()
                        .code(rs.getString("stock_code")).name(rs.getString("name"))
                        .market(rs.getString("market")).build();
            }
        } catch (SQLException e) { /* ignore */ }
        return null;
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
}
