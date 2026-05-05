package com.stock.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.model.KLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * Redis K线缓存——将每只股票的全部日K线以JSON格式存储在Redis中。
 * <p>
 * Key: kl:{stockCode}  Value: JSON数组
 * Redis不可用时自动回退到SQLite直查。
 */
public class RedisKLineCache implements KLineCache {

    private static final Logger log = LoggerFactory.getLogger(RedisKLineCache.class);
    private static final String KEY_PREFIX = "kl:";

    private final ObjectMapper mapper = new ObjectMapper();
    private final String host;
    private final int port;
    private final String password;
    private boolean available;

    public RedisKLineCache(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.available = testConnection();
    }

    private boolean testConnection() {
        try (Jedis jedis = connect()) {
            jedis.ping();
            log.info("Redis连接成功: {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.warn("Redis不可用，回退到SQLite直查: {}", e.getMessage());
            return false;
        }
    }

    private Jedis connect() {
        Jedis jedis = new Jedis(host, port, 2000);
        if (password != null && !password.isBlank()) {
            jedis.auth(password);
        }
        return jedis;
    }

    @Override
    public boolean isAvailable() { return available; }

    @Override
    public List<KLine> get(String stockCode) {
        if (!isAvailable()) return null;
        try (Jedis jedis = connect()) {
            String json = jedis.get(KEY_PREFIX + stockCode);
            if (json == null || json.isEmpty()) return null;
            return mapper.readValue(json, new TypeReference<List<KLine>>() {});
        } catch (Exception e) {
            log.debug("Redis读取失败 {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    @Override
    public void put(String stockCode, List<KLine> klines) {
        if (!isAvailable() || klines == null || klines.isEmpty()) return;
        try (Jedis jedis = connect()) {
            String json = mapper.writeValueAsString(klines);
            jedis.set(KEY_PREFIX + stockCode, json);
        } catch (Exception e) {
            log.debug("Redis写入失败 {}: {}", stockCode, e.getMessage());
        }
    }
}
