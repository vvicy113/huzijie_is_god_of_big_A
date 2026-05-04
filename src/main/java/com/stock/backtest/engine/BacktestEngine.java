package com.stock.backtest.engine;

import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.backtest.strategy.Strategy;
import com.stock.model.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 回测引擎 — 短线多股票回测（开发中）。
 * <p>
 * TODO: 买入/卖出执行逻辑待定。
 */
public class BacktestEngine {

    private static final double COMMISSION_RATE = 0.00025;
    private static final double MIN_COMMISSION = 5.0;
    private static final double STAMP_TAX_RATE = 0.001;

    public BacktestResult run(Strategy strategy,
                              Map<String, List<KLine>> klineMap,
                              double initialCapital,
                              LocalDate from, LocalDate to) {
        strategy.onReset();

        // TODO: 实现短线多股票回测逻辑
        throw new UnsupportedOperationException("回测引擎正在重构中，暂不可用");
    }
}
