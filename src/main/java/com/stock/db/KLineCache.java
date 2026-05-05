package com.stock.db;

import com.stock.model.KLine;

import java.util.List;

/**
 * K线缓存接口——在 SQLite 前加一层缓存。
 * <p>
 * 缓存以股票为单位存储全部日K线，日期过滤在 Java 层做。
 * 用于加速批量回测场景（同一只股票被重复查询）。
 */
public interface KLineCache {

    /**
     * 获取某股票的全部日K线。缓存未命中返回 null。
     */
    List<KLine> get(String stockCode);

    /**
     * 存入某股票的全部日K线。
     */
    void put(String stockCode, List<KLine> klines);

    /**
     * 缓存是否可用（Redis未配置时返回false，自动回退SQLite）。
     */
    default boolean isAvailable() { return true; }
}
