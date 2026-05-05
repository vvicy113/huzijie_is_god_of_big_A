package com.stock;

import com.stock.batch.BatchRunner;
import com.stock.cli.ConsoleUI;
import com.stock.config.ConfigGeneratorUtils;
import com.stock.log.LogSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LogSetup.init();

        if (args.length > 0 && "--batch".equals(args[0])) {
            runBatch();
        } else {
            new ConsoleUI().start();
        }
    }

    private static void runBatch() {
        log.info(ConfigGeneratorUtils.describeSpace());
        var configs = ConfigGeneratorUtils.generateAll();
        try {
            new BatchRunner(configs,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2026, 4, 30),
                    100_000).run();
        } catch (Exception e) {
            log.error("批量回测失败", e);
        }
    }
}
