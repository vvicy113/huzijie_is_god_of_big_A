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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class BacktestMenu {

    private static final Logger log = LoggerFactory.getLogger(BacktestMenu.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Scanner scanner;

    public BacktestMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        log.info("\n  ══════════════ 回测引擎 ══════════════");

        List<String> names = StrategyRegistry.getStrategyNames();
        if (names.isEmpty()) {
            log.warn("  当前没有已注册的策略。");
            System.out.print("  按回车键返回主菜单...");
            scanner.nextLine();
            return;
        }

        log.info("  可用策略:");
        for (int i = 0; i < names.size(); i++) log.info("    {}  {}", i + 1, names.get(i));
        System.out.print("  选择策略 [1-" + names.size() + "]: ");
        Strategy strategy = null;
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx >= 0 && idx < names.size()) strategy = StrategyRegistry.getByName(names.get(idx));
        } catch (NumberFormatException ignored) {}
        if (strategy == null) { log.warn("  无效选择。"); return; }

        LocalDate from = null, to = null;
        System.out.print("  起始日期（yyyy-MM-dd，留空=全部）: ");
        String fromStr = scanner.nextLine().trim();
        if (!fromStr.isEmpty()) {
            try { from = LocalDate.parse(fromStr, DATE_FMT); }
            catch (DateTimeParseException e) { log.warn("  日期格式无效。"); return; }
        }
        System.out.print("  结束日期（yyyy-MM-dd，留空=全部）: ");
        String toStr = scanner.nextLine().trim();
        if (!toStr.isEmpty()) {
            try { to = LocalDate.parse(toStr, DATE_FMT); }
            catch (DateTimeParseException e) { log.warn("  日期格式无效。"); return; }
        }
        if (from == null) from = LocalDate.of(2020, 1, 1);
        if (to == null) to = LocalDate.now();

        BacktestResult result = new BacktestEngine().run(strategy, from, to);
        if (result != null) log.info(new ReportGenerator().generate(result));

        System.out.print("  按回车键返回主菜单...");
        scanner.nextLine();
    }

    /** 自动模式 */
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
