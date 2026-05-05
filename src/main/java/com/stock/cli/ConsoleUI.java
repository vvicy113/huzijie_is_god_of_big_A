package com.stock.cli;

import com.stock.config.RunConfig;
import com.stock.log.LogSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class ConsoleUI {

    private static final Logger log = LoggerFactory.getLogger(ConsoleUI.class);

    public void start() {
        InputStream in = ConsoleUI.class.getClassLoader().getResourceAsStream("run.properties");
        if (in == null) {
            log.error("配置文件 run.properties 不存在");
            log.error("请将 run.properties.example 复制为 run.properties 并修改参数");
            return;
        }
        RunConfig config = RunConfig.load(in);
        LogSetup.writeConfigSummary(config.mode(), config.strategyName());
        log.info("运行模式: {}", config.mode());

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
