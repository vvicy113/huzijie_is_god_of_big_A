package com.stock.batch;

import com.stock.db.KLineCache;
import com.stock.db.KLineRepository;
import com.stock.model.KLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Redis 预加载器——启动时将 SQLite 中全部日K线批量写入 Redis。
 */
public class RedisPreloader {

    private static final Logger log = LoggerFactory.getLogger(RedisPreloader.class);

    public static void preload(KLineRepository repo, KLineCache cache) {
        if (!cache.isAvailable()) {
            log.info("缓存不可用，跳过预加载");
            return;
        }

        List<String> codes = repo.getAllStockCodes();
        log.info("开始预加载 {} 只股票到Redis...", codes.size());

        int loaded = 0;
        long start = System.currentTimeMillis();
        for (String code : codes) {
            List<KLine> klines = repo.findByStockCode(code);
            if (!klines.isEmpty()) {
                cache.put(code, klines);
                loaded++;
            }
            if (loaded % 500 == 0) {
                log.info("  预加载进度: {}/{}", loaded, codes.size());
            }
        }

        long elapsed = (System.currentTimeMillis() - start) / 1000;
        log.info("预加载完成: {} 只股票, 耗时 {}秒", loaded, elapsed);
    }
}
