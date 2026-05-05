package com.stock.db;

import com.stock.model.KLine;

import java.util.List;

/**
 * 空缓存实现——不缓存任何数据，每次都查 SQLite。
 * Redis 不可用时的默认回退。
 */
public class NoopKLineCache implements KLineCache {
    @Override public List<KLine> get(String stockCode) { return null; }
    @Override public void put(String stockCode, List<KLine> klines) {}
    @Override public boolean isAvailable() { return false; }
}
