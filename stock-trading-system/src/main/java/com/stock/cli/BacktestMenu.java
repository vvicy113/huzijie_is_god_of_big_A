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

        // 输入股票代码
        System.out.println("  请输入股票代码（如 600519），程序自动从桌面 stocks/日k/ 加载数据");
        System.out.println("  也可输入完整CSV文件路径用于自定义数据:");
        System.out.print("  > ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            System.out.println("  输入不能为空。");
            return;
        }

        // 加载数据：判断是股票代码还是文件路径
        List<KLine> klineData;
        String stockCode;
        try {
            if (input.contains("/") || input.contains("\\") || input.endsWith(".csv")) {
                // 完整文件路径
                klineData = loader.load(input);
                String fileName = input.substring(input.lastIndexOf('/') + 1);
                stockCode = extractCodeFromFileName(fileName);
            } else {
                // 纯股票代码，从桌面 stocks 加载
                stockCode = input;
                klineData = loader.loadFromDesktopStocks(stockCode);
            }
            System.out.println("  成功加载 " + klineData.size() + " 条K线数据。");
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

    private static String extractCodeFromFileName(String fileName) {
        // 从 sh600519.csv、SH600519.csv 等提取股票代码
        String name = fileName.replace(".csv", "").replace(".CSV", "");
        if (name.length() >= 2 && Character.isLetter(name.charAt(0))) {
            return name.substring(2); // 去掉 sh/sz/bj 前缀
        }
        return name;
    }
}
