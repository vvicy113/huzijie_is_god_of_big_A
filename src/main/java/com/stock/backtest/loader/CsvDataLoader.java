package com.stock.backtest.loader;

import com.stock.db.KLineRepository;
import com.stock.model.KLine;

import java.io.*;
import java.util.List;

/**
 * 日K线数据加载器。从 SQLite 数据库读取日K线数据。
 */
public class CsvDataLoader {

    private final KLineRepository dbRepo;

    public CsvDataLoader() {
        this.dbRepo = new KLineRepository();
    }

    /**
     * 从数据库加载指定股票的日K线数据。
     */
    public List<KLine> loadDailyKLine(String stockCode) throws IOException {
        if (dbRepo.exists(stockCode)) {
            List<KLine> data = dbRepo.findByStockCode(stockCode);
            if (!data.isEmpty()) {
                return data;
            }
        }
        throw new FileNotFoundException(
                "数据库中无股票 " + stockCode + " 的数据，请先在「数据管理」中导入CSV。");
    }
}
