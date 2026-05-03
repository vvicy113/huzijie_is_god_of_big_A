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
        System.out.println("  1. 导入日K线到数据库（从桌面 stocks/日k/）");
        System.out.println("  2. 导入分时数据到数据库（从 csv/ 年份目录）");
        System.out.println("  3. 查看数据库统计");
        System.out.println("  0. 返回主菜单");
        System.out.println();
        System.out.print("  请选择 [0-3]: ");

        String input = scanner.nextLine().trim();
        switch (input) {
            case "1" -> doImportDaily();
            case "2" -> doImportMinute();
            case "3" -> showStats();
            case "0" -> {}
            default -> System.out.println("  无效输入。");
        }
    }

    private void doImportDaily() {
        String dbPath = new File("data/stocks.db").getAbsolutePath();
        System.out.println();
        System.out.println("  数据源: ~/Desktop/stocks/日k/");
        System.out.println("  目标库: " + dbPath);
        System.out.println();
        System.out.print("  确认导入？这可能需要几分钟 [y/N]: ");
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

    private void doImportMinute() {
        System.out.println();
        System.out.println("  分时数据源: csv/ 目录下的年份子目录");
        System.out.println("  可选年份: 2023, 2024, 2025");
        System.out.println();
        System.out.print("  请输入年份（all=全部，2023/2024/2025）: ");
        String year = scanner.nextLine().trim().toLowerCase();

        if (year.isEmpty()) {
            System.out.println("  已取消。");
            return;
        }

        String[] years;
        if (year.equals("all")) {
            years = new String[]{"2023", "2024", "2025"};
        } else {
            years = new String[]{year};
        }

        System.out.print("  确认导入？这可能需要几分钟 [y/N]: ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("y") && !confirm.equals("yes")) {
            System.out.println("  已取消。");
            return;
        }

        System.out.println();
        try {
            CsvImporter importer = new CsvImporter();
            for (String y : years) {
                CsvImporter.ImportResult result = importer.importMinuteData(y);
                System.out.printf("  [%s] 成功: %d 文件, 失败: %d 文件, 行数: %d%n",
                        y, result.successFiles(), result.failedFiles(), result.totalRows());
            }
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
        System.out.println();
        System.out.println("  [日K线]");
        System.out.printf("    股票数:  %d%n", repo.getStockCount());
        System.out.printf("    总行数:  %d%n", repo.getTotalRows());
        System.out.println();
        System.out.println("  [分时数据]");
        System.out.printf("    总行数:  %d%n", repo.getMinuteTotalRows());
        System.out.println("  ──────────────────────────────────────");

        System.out.print("\n  按回车键返回...");
        scanner.nextLine();
    }
}
