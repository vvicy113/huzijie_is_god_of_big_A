package com.stock.cli;

import com.stock.config.RunConfig;
import com.stock.db.CsvImporter;
import com.stock.db.KLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Scanner;

public class DataMenu {

    private static final Logger log = LoggerFactory.getLogger(DataMenu.class);

    private final Scanner scanner;

    public DataMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        log.info("\n  ══════════════ 数据管理 ══════════════\n");
        log.info("  1. 导入CSV到数据库（日k → SQLite，仅sh/sz主板股票）");
        log.info("  2. 查看数据库统计");
        log.info("  0. 返回主菜单");
        System.out.print("  请选择 [0-2]: ");

        String input = scanner.nextLine().trim();
        switch (input) {
            case "1" -> doImport();
            case "2" -> showStats();
            case "0" -> {}
            default -> log.warn("  无效输入。");
        }
    }

    private void doImport() {
        String dbPath = new File("data/stocks.db").getAbsolutePath();
        log.info("\n  数据源: csv/日k/");
        log.info("  过滤条件: 仅 sh/sz 前缀 + 主板（通过 股票列表.csv 筛选）");
        log.info("  目标库: {}", dbPath);
        System.out.print("  确认导入？数据约 1.3GB，可能需要几分钟 [y/N]: ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) { log.info("  已取消。"); return; }

        try {
            CsvImporter importer = new CsvImporter();
            CsvImporter.ImportResult result = importer.importAll();
            log.info("\n  ──────────────────────────────────────");
            log.info("  导入结果:  成功文件: {}  失败文件: {}  总行数: {}  数据库: {}",
                    result.successFiles(), result.failedFiles(), result.totalRows(), dbPath);
            log.info("  ──────────────────────────────────────");
        } catch (Exception e) {
            log.error("  导入失败: {}", e.getMessage());
        }
        System.out.print("  按回车键返回...");
        scanner.nextLine();
    }

    public void showStats() {
        KLineRepository repo = new KLineRepository();
        String dbPath = new File("data/stocks.db").getAbsolutePath();
        long fileSize = new File("data/stocks.db").length();
        log.info("\n  ──────────────────────────────────────");
        log.info("  数据库: {}  文件大小: {:.1f} MB  股票数: {}  总K线数: {}",
                dbPath, fileSize / 1024.0 / 1024.0, repo.getStockCount(), repo.getTotalRows());
        log.info("  ──────────────────────────────────────");

        if (scanner != null) {
            System.out.print("  按回车键返回...");
            scanner.nextLine();
        }
    }

    /** 自动模式导入 */
    public void runImportAuto(RunConfig config) {
        try {
            CsvImporter importer = new CsvImporter();
            CsvImporter.ImportResult result = importer.importAll();
            log.info("导入完成: 成功 {} 文件, 失败 {} 文件, 行数 {}",
                    result.successFiles(), result.failedFiles(), result.totalRows());
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage());
        }
    }
}
