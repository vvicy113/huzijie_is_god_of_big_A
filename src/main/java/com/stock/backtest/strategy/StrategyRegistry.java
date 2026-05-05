package com.stock.backtest.strategy;

import java.util.*;

/**
 * 策略注册表。
 * <p>
 * 策略通过 run.properties 配置文件的 selector.codes + scorer.codes 自由组装，
 * 不再需要在此处硬编码注册。此注册表留作扩展点。
 */
public class StrategyRegistry {

    private static final Map<String, Strategy> strategies = new LinkedHashMap<>();

    public static void register(Strategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }

    public static List<String> getStrategyNames() {
        return List.copyOf(strategies.keySet());
    }

    public static Strategy getByName(String name) {
        return strategies.get(name);
    }
}
