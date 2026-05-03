package com.stock.backtest.strategy;

import java.util.*;

public class StrategyRegistry {

    private static final Map<String, Strategy> strategies = new LinkedHashMap<>();

    static {
        register(new MovingAverageCrossStrategy(5, 20));
        register(new MovingAverageCrossStrategy(10, 30));
        register(new GoldenCrossStrategy());
        register(new BreakoutStrategy(20));
        register(new BreakoutStrategy(60));
    }

    public static void register(Strategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }

    public static List<String> getStrategyNames() {
        return List.copyOf(strategies.keySet());
    }

    public static Strategy getByName(String name) {
        return strategies.get(name);
    }

    public static List<Strategy> getAll() {
        return List.copyOf(strategies.values());
    }
}
