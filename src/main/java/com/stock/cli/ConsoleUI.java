package com.stock.cli;

import com.stock.config.RunConfig;
import com.stock.db.ProjectPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.Scanner;

public class ConsoleUI {

    private static final Logger log = LoggerFactory.getLogger(ConsoleUI.class);

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
        var configFile = ProjectPaths.resolve("run.properties");
        if (Files.exists(configFile)) {
            RunConfig config = RunConfig.load(configFile);
            runAuto(config);
        } else {
            runInteractive();
        }
    }

    private void runAuto(RunConfig config) {
        log.info("运行模式: 自动（读取 run.properties） mode={}", config.mode());
        switch (config.mode().toLowerCase()) {
            case "backtest" -> backtestMenu.runAuto(config);
            case "analysis" -> analysisMenu.runAuto(config);
            case "import" -> dataMenu.runImportAuto(config);
            case "stats" -> dataMenu.showStats();
            default -> log.warn("未知运行模式: {}", config.mode());
        }
        log.info("执行完毕");
    }

    private void runInteractive() {
        while (true) {
            log.info("\n  ╔══════════════════════════════════════════╗\n"
                   + "  ║        股票回测与分析系统  v1.0         ║\n"
                   + "  ╚══════════════════════════════════════════╝\n");
            log.info("  1. 回测引擎    — 加载日K线，用策略回测历史收益");
            log.info("  2. 股票关联分析 — 联网抓取板块/题材/分时数据，分析股票关系");
            log.info("  3. 数据管理    — 导入CSV到SQLite数据库 / 查看统计");
            log.info("  0. 退出");
            System.out.print("  请选择 [0-3]: ");

            String input = scanner.nextLine().trim();
            switch (input) {
                case "1" -> backtestMenu.show();
                case "2" -> analysisMenu.show();
                case "3" -> dataMenu.show();
                case "0" -> { log.info("  感谢使用，再见！"); return; }
                default -> log.warn("  无效输入，请重新选择。");
            }
        }
    }
}
