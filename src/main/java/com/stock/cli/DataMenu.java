package com.stock.cli;

import com.stock.db.CsvImporter;
import com.stock.db.KLineRepository;

import java.io.File;
import java.util.Scanner;

public class DataMenu {

    private final Scanner scanner;

    public DataMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println();
        System.out.println("  ══════════════ 数据管理 ══════════════");
        System.out.println();
        System.out.println("  1. 导入CSV到数据库（日k → SQLite，仅sh/sz主板股票）");
        System.out.println("  2. 查看数据库统计");
        System.out.println("  0. 返回主菜单");
        System.out.println();
        System.out.print("  请选择 [0-2]: ");

        String input = scanner.nextLine().trim();
        switch (input) {
            case "1" -> doImport();
            case "2" -> showStats();
            case "0" -> {}
            default -> System.out.println("  无效输入。");
        }
    }

    private void doImport() {
        String dbPath = new File("data/stocks.db").getAbsolutePath();
        System.out.println();
        System.out.println("  数据源: csv/日k/");
        System.out.println("  过滤条件: 仅 sh/sz 前缀 + 主板（通过 股票列表.csv 筛选）");
        System.out.println("  目标库: " + dbPath);
        System.out.println();
        System.out.print("  确认导入？数据约 1.3GB，可能需要几分钟 [y/N]: ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  已取消。");
            return;
        }

        System.out.println();
        try {
            CsvImporter importer = new CsvImporter();
            CsvImporter.ImportResult result = importer.importAll();

            System.out.println();
            System.out.println("  ──────────────────────────────────────");
            System.out.printf("  导入结果:%n");
            System.out.printf("    成功文件: %d%n", result.successFiles());
            System.out.printf("    失败文件: %d%n", result.failedFiles());
            System.out.printf("    总行数:   %d%n", result.totalRows());
            System.out.printf("    数据库:   %s%n", dbPath);
            System.out.println("  ──────────────────────────────────────");
        } catch (Exception e) {
            System.out.println("  导入失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.print("\n  按回车键返回...");
        scanner.nextLine();
    }

    private void showStats() {
        System.out.println();
        KLineRepository repo = new KLineRepository();

        String dbPath = new File("data/stocks.db").getAbsolutePath();
        long fileSize = new File("data/stocks.db").length();

        System.out.println("  ──────────────────────────────────────");
        System.out.printf("  数据库:  %s%n", dbPath);
        System.out.printf("  文件大小: %.1f MB%n", fileSize / 1024.0 / 1024.0);
        System.out.printf("  股票数:  %d%n", repo.getStockCount());
        System.out.printf("  总K线数: %d%n", repo.getTotalRows());
        System.out.println("  ──────────────────────────────────────");

        System.out.print("\n  按回车键返回...");
        scanner.nextLine();
    }
}
