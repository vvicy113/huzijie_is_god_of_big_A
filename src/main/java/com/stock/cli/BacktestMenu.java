package com.stock.cli;

import com.stock.backtest.engine.BacktestEngine;
import com.stock.backtest.loader.CsvDataLoader;
import com.stock.backtest.report.ReportGenerator;
import com.stock.backtest.strategy.Strategy;
import com.stock.backtest.strategy.StrategyRegistry;
import com.stock.model.BacktestResult;
import com.stock.model.KLine;

import java.util.List;
import java.util.Scanner;

public class BacktestMenu {

    private final Scanner scanner;
    private final CsvDataLoader loader;
    private final BacktestEngine engine;
    private final ReportGenerator reportGen;

    public BacktestMenu(Scanner scanner) {
        this.scanner = scanner;
        this.loader = new CsvDataLoader();
        this.engine = new BacktestEngine();
        this.reportGen = new ReportGenerator();
    }

    public void show() {
        System.out.println();
        System.out.println("  ══════════════ 回测引擎 ══════════════");
        System.out.println();

        System.out.print("  请输入股票代码（如 600519）: ");
        String stockCode = scanner.nextLine().trim();

        if (stockCode.isEmpty()) {
            System.out.println("  输入不能为空。");
            return;
        }

        // 从数据库加载日K线
        List<KLine> klineData;
        try {
            klineData = loader.loadDailyKLine(stockCode);
            System.out.println("  成功加载 " + klineData.size() + " 条日K线数据。");
        } catch (Exception e) {
            System.out.println("  加载失败: " + e.getMessage());
            return;
        }

        // 选择策略
        List<String> strategyNames = StrategyRegistry.getStrategyNames();
        System.out.println("\n  可用策略:");
        for (int i = 0; i < strategyNames.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + strategyNames.get(i));
        }
        System.out.print("  选择策略序号 [1-" + strategyNames.size() + "]: ");
        Strategy strategy = null;
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx >= 0 && idx < strategyNames.size()) {
                strategy = StrategyRegistry.getByName(strategyNames.get(idx));
            }
        } catch (NumberFormatException ignored) {}

        if (strategy == null) {
            System.out.println("  无效的策略选择。");
            return;
        }

        // 初始资金
        System.out.print("  初始资金 [默认100000]: ");
        double initialCapital = 100000;
        String capitalInput = scanner.nextLine().trim();
        if (!capitalInput.isEmpty()) {
            try {
                initialCapital = Double.parseDouble(capitalInput);
            } catch (NumberFormatException e) {
                System.out.println("  输入无效，使用默认值100000。");
            }
        }

        // 运行回测
        System.out.println("\n  正在运行回测...");
        BacktestResult result = engine.run(strategy, klineData, initialCapital, stockCode);

        // 输出报告
        String report = reportGen.generate(result);
        System.out.println(report);
        System.out.print("\n  按回车键返回主菜单...");
        scanner.nextLine();
    }
}
