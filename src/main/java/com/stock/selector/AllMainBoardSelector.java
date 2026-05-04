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

/**
 * 全量主板股票选择器——责任链的起始节点。
 * <p>
 * 忽略输入 candidates 和 date，直接从 daily_kline 查询所有已入库的 sh/sz 主板股票。
 */
public class AllMainBoardSelector implements StockSelector {

    @Override
    public int getCode() {
        return SelectorConstants.ALL_MAIN_BOARD;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        String sql = "SELECT DISTINCT stock_code FROM daily_kline ORDER BY stock_code";
        List<String> result = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("stock_code"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询全量主板股票失败", e);
        }
        return result;
    }
}
