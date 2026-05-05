package com.stock.cli;

import com.stock.config.RunConfig;
import com.stock.db.ProjectPaths;
import com.stock.log.LogSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

public class ConsoleUI {

    private static final Logger log = LoggerFactory.getLogger(ConsoleUI.class);

    public void start() {
        var configFile = ProjectPaths.resolve("run.properties");
        if (!Files.exists(configFile)) {
            log.error("配置文件不存在: {}", configFile);
            log.error("请复制 run.properties.example 为 run.properties 并修改参数");
            return;
        }
        RunConfig config = RunConfig.load(configFile);
        LogSetup.writeConfigSummary(config.mode(), config.strategyName());
        log.info("运行模式: {}  配置文件: {}", config.mode(), configFile);

        switch (config.mode().toLowerCase()) {
            case "backtest" -> new BacktestMenu().runAuto(config);
            case "analysis" -> new AnalysisMenu().runAuto(config);
            case "import"   -> new DataMenu().runImportAuto(config);
            case "stats"    -> new DataMenu().showStats();
            default -> log.warn("未知运行模式: {}", config.mode());
        }
        log.info("执行完毕");
    }
}
