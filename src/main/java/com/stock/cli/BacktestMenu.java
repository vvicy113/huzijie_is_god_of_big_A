package com.stock.cli;

import com.stock.backtest.engine.BacktestEngine;
import com.stock.backtest.report.ReportGenerator;
import com.stock.backtest.strategy.Strategy;
import com.stock.backtest.strategy.StrategyRegistry;
import com.stock.model.BacktestResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class BacktestMenu {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Scanner scanner;
    private final BacktestEngine engine;

    public BacktestMenu(Scanner scanner) {
        this.scanner = scanner;
        this.engine = new BacktestEngine();
    }

    public void show() {
        System.out.println();
        System.out.println("  ══════════════ 回测引擎 ══════════════");
        System.out.println();

        // 选择策略
        List<String> names = StrategyRegistry.getStrategyNames();
        if (names.isEmpty()) {
            System.out.println("  当前没有已注册的策略。");
            System.out.print("  按回车键返回主菜单...");
            scanner.nextLine();
            return;
        }

        System.out.println("  可用策略:");
        for (int i = 0; i < names.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + names.get(i));
        }
        System.out.print("  选择策略 [1-" + names.size() + "]: ");
        Strategy strategy = null;
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx >= 0 && idx < names.size()) {
                strategy = StrategyRegistry.getByName(names.get(idx));
            }
        } catch (NumberFormatException ignored) {}

        if (strategy == null) {
            System.out.println("  无效选择。");
            return;
        }

        // 日期范围
        LocalDate from = null, to = null;
        System.out.print("  起始日期（yyyy-MM-dd，留空=全部）: ");
        String fromStr = scanner.nextLine().trim();
        if (!fromStr.isEmpty()) {
            try { from = LocalDate.parse(fromStr, DATE_FMT); }
            catch (DateTimeParseException e) { System.out.println("  日期格式无效。"); return; }
        }
        System.out.print("  结束日期（yyyy-MM-dd，留空=全部）: ");
        String toStr = scanner.nextLine().trim();
        if (!toStr.isEmpty()) {
            try { to = LocalDate.parse(toStr, DATE_FMT); }
            catch (DateTimeParseException e) { System.out.println("  日期格式无效。"); return; }
        }

        if (from == null) from = LocalDate.of(2020, 1, 1);
        if (to == null) to = LocalDate.now();

        BacktestResult result = engine.run(strategy, from, to);
        if (result != null) {
            System.out.println(new ReportGenerator().generate(result));
        }

        System.out.print("\n  按回车键返回主菜单...");
        scanner.nextLine();
    }
}
