package com.stock.backtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CsvDataLoader 测试。需要数据库中有数据时才能完整测试。
 */
class CsvDataLoaderTest {

    @Test
    void shouldRejectEmptyStockCode() {
        // 空股票代码应在 repo 层抛出异常或返回 false
        com.stock.db.KLineRepository repo = new com.stock.db.KLineRepository();
        assertFalse(repo.exists(""));
    }
}
