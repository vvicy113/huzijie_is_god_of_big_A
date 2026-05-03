package com.stock.cli;

import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner;
    private final BacktestMenu backtestMenu;
    private final AnalysisMenu analysisMenu;
    private final DataMenu dataMenu;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
        this.backtestMenu = new BacktestMenu(scanner);
        this.analysisMenu = new AnalysisMenu(scanner);
        this.dataMenu = new DataMenu(scanner);
    }

    public void start() {
        while (true) {
            printHeader();
            System.out.println("  1. 回测引擎    — 加载日K线，用策略回测历史收益");
            System.out.println("  2. 股票关联分析 — 联网抓取板块/题材/分时数据，分析股票关系");
            System.out.println("  3. 数据管理    — 导入CSV到SQLite数据库 / 查看统计");
            System.out.println("  0. 退出");
            System.out.println();
            System.out.print("  请选择 [0-3]: ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1" -> backtestMenu.show();
                case "2" -> analysisMenu.show();
                case "3" -> dataMenu.show();
                case "0" -> {
                    System.out.println("\n  感谢使用，再见！");
                    return;
                }
                default -> System.out.println("  无效输入，请重新选择。");
            }
        }
    }

    private void printHeader() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║        股票回测与分析系统  v1.0         ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println();
    }
}
