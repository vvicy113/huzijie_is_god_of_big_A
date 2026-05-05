package com.stock.backtest.strategy;

import com.stock.constants.ComparatorConstants;
import com.stock.constants.SelectorConstants;

import java.util.*;

public class StrategyRegistry {

    private static final Map<String, Strategy> strategies = new LinkedHashMap<>();

    // 常用选股器组合
    private static final int[] SEL_STANDARD = {SelectorConstants.ALL_MAIN_BOARD,
            SelectorConstants.VOL_20_1P5, SelectorConstants.MA_5_20};
    private static final int[] SEL_AGGRESSIVE = {SelectorConstants.ALL_MAIN_BOARD,
            SelectorConstants.VOL_10_2P0, SelectorConstants.MOMENTUM_10_5_30};
    private static final int[] SEL_REVERSAL = {SelectorConstants.ALL_MAIN_BOARD,
            SelectorConstants.POS_RANK_LOW_20, SelectorConstants.BOLL_LOWER_20_2};

    // 常用打分器组合
    private static final int[] SCR_MOM_VOL =
            {ComparatorConstants.MOMENTUM_10, ComparatorConstants.VOL_RATIO_20, ComparatorConstants.MA_DIST_5};
    private static final double[] SCR_MOM_VOL_W = {0.5, 0.3, 0.2};

    private static final int[] SCR_AGGRESSIVE =
            {ComparatorConstants.MOMENTUM_10, ComparatorConstants.VOL_RATIO_5, ComparatorConstants.AMPLITUDE};
    private static final double[] SCR_AGGRESSIVE_W = {0.6, 0.25, 0.15};

    private static final int[] SCR_BALANCED =
            {ComparatorConstants.MOMENTUM_20, ComparatorConstants.VOL_RATIO_20, ComparatorConstants.MA_DIST_20};
    private static final double[] SCR_BALANCED_W = {0.4, 0.3, 0.3};

    static {
        register(new MomentumBreakoutStrategy("动量突破(标准)",  SEL_STANDARD,    SCR_MOM_VOL,    SCR_MOM_VOL_W,    60));
        register(new MomentumBreakoutStrategy("动量突破(激进)",  SEL_AGGRESSIVE,  SCR_AGGRESSIVE, SCR_AGGRESSIVE_W, 70));
        register(new MomentumBreakoutStrategy("动量突破(稳健)",  SEL_STANDARD,    SCR_BALANCED,   SCR_BALANCED_W,   50));
        register(new MomentumBreakoutStrategy("底部反转",       SEL_REVERSAL,    SCR_MOM_VOL,    SCR_MOM_VOL_W,    65));
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
