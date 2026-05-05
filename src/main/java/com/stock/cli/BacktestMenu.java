package com.stock.cli;

import com.stock.backtest.engine.BacktestEngine;
import com.stock.backtest.report.ReportGenerator;
import com.stock.backtest.strategy.Strategy;
import com.stock.backtest.strategy.StrategyRegistry;
import com.stock.config.RunConfig;
import com.stock.model.BacktestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class BacktestMenu {

    private static final Logger log = LoggerFactory.getLogger(BacktestMenu.class);

    public void runAuto(RunConfig config) {
        String name = config.strategyName();
        Strategy strategy = StrategyRegistry.getByName(name);
        if (strategy == null) {
            log.warn("策略不存在: {}  可用: {}", name, StrategyRegistry.getStrategyNames());
            return;
        }
        LocalDate from = config.dateFrom() != null ? config.dateFrom() : LocalDate.of(2020, 1, 1);
        LocalDate to = config.dateTo() != null ? config.dateTo() : LocalDate.now();

        BacktestResult result = new BacktestEngine(config.backtest()).run(strategy, from, to);
        if (result != null) log.info(new ReportGenerator().generate(result));
    }
}
