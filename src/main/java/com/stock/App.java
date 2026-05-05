package com.stock;

import com.stock.batch.BatchRunner;
import com.stock.cli.ConsoleUI;
import com.stock.config.ConfigGeneratorUtils;
import com.stock.db.KLineCache;
import com.stock.db.KLineRepository;
import com.stock.db.NoopKLineCache;
import com.stock.db.RedisKLineCache;
import com.stock.log.LogSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LogSetup.init();
        initRedis();

        if (args.length > 0 && "--batch".equals(args[0])) {
            runBatch();
        } else {
            new ConsoleUI().start();
        }
    }

    /** 初始化Redis缓存（不可用时自动回退） */
    private static void initRedis() {
        String host = getEnvOrProp("redis.host", "localhost");
        int port = Integer.parseInt(getEnvOrProp("redis.port", "6379"));
        String password = getEnvOrProp("redis.password", "");

        KLineCache cache;
        if (!"localhost".equals(host) || System.getenv("REDIS_HOST") != null) {
            cache = new RedisKLineCache(host, port, password);
        } else {
            cache = new NoopKLineCache();
        }
        KLineRepository.setGlobalCache(cache);
    }

    private static void runBatch() {
        log.info(ConfigGeneratorUtils.describeSpace());
        var configs = ConfigGeneratorUtils.generateAll();
        try {
            new BatchRunner(configs,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2026, 4, 30),
                    100_000).run();
        } catch (Exception e) {
            log.error("批量回测失败", e);
        }
    }

    private static String getEnvOrProp(String key, String def) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null) return env;
        return System.getProperty(key, def);
    }
}
