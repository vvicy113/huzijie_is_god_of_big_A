package com.stock.db;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV 数据批量导入 SQLite。支持日K线（UTF-8）和分时数据（GBK）。
 */
public class CsvImporter {

    private static final String CSV_DIR =
            System.getProperty("user.home") + "/Desktop/stocks/日k";
    private static final String MINUTE_CSV_DIR = "csv";
    private static final Charset GBK = Charset.forName("GBK");
    private static final int BATCH_SIZE = 5000;

    private final DatabaseManager dbManager;

    public CsvImporter() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * 导入全部日K线 CSV 文件（从桌面 stocks/日k/）。
     *
     * @return 导入统计信息
     */
    public ImportResult importAll() throws IOException {
        Path dir = Paths.get(CSV_DIR);
        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException("CSV目录不存在: " + CSV_DIR);
        }

        List<Path> csvFiles;
        try (var stream = Files.list(dir)) {
            csvFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }

        if (csvFiles.isEmpty()) {
            System.out.println("  未找到CSV文件。");
            return new ImportResult(0, 0, 0);
        }

        System.out.println("  找到 " + csvFiles.size() + " 个CSV文件，开始导入...");
        System.out.println("  导入期间会关闭同步写入以加速，完成后自动恢复。");
        System.out.println();

        dbManager.setBulkImportMode();

        int totalFiles = csvFiles.size();
        int successFiles = 0;
        int failedFiles = 0;
        long totalRows = 0;
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "INSERT OR IGNORE INTO daily_kline " +
                         "(stock_code, date, open, high, low, close, volume, amount) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int batchCount = 0;

                for (int i = 0; i < csvFiles.size(); i++) {
                    Path file = csvFiles.get(i);
                    String fileName = file.getFileName().toString();

                    try {
                        long rows = importOneFile(file, ps);
                        batchCount += rows;
                        totalRows += rows;
                        successFiles++;

                        // 每 BATCH_SIZE 行提交一次
                        if (batchCount >= BATCH_SIZE) {
                            ps.executeBatch();
                            conn.commit();
                            batchCount = 0;
                        }
                    } catch (Exception e) {
                        failedFiles++;
                        System.err.println("    导入失败: " + fileName + " - " + e.getMessage());
                    }

                    // 进度显示：每 100 个文件打一次点
                    if ((i + 1) % 100 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double pct = (i + 1) * 100.0 / totalFiles;
                        System.out.printf("  进度: %d/%d (%.1f%%), 已用时: %.1f秒, 已导入: %d行%n",
                                i + 1, totalFiles, pct, elapsed / 1000.0, totalRows);
                    }
                }

                // 提交剩余批次
                if (batchCount > 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库写入失败", e);
        } finally {
            dbManager.setNormalMode();
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf("%n  导入完成！成功: %d 文件, 失败: %d 文件, 总行数: %d, 耗时: %d秒%n",
                successFiles, failedFiles, totalRows, elapsed);

        return new ImportResult(successFiles, failedFiles, totalRows);
    }

    private long importOneFile(Path file, PreparedStatement ps) throws Exception {
        String fileName = file.getFileName().toString();
        String stockCode = extractStockCode(fileName);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8));
             CSVReader reader = new CSVReaderBuilder(br).build()) {

            // 跳过表头行
            String[] header = reader.readNext();
            if (header == null) {
                return 0;
            }

            long rows = 0;
            String[] fields;
            while ((fields = reader.readNext()) != null) {
                if (fields.length < 8) continue;

                // fields: [0]=股票代码, [1]=日期, [2]=开盘价, [3]=最高价, [4]=最低价,
                //          [5]=收盘价, [6]=昨收价, [7]=涨跌额, [8]=涨跌幅, [9]=成交量, [10]=成交额
                ps.setString(1, stockCode);
                ps.setString(2, fields[1]);           // 日期
                ps.setDouble(3, parseDouble(fields[2]));  // 开盘价
                ps.setDouble(4, parseDouble(fields[3]));  // 最高价
                ps.setDouble(5, parseDouble(fields[4]));  // 最低价
                ps.setDouble(6, parseDouble(fields[5]));  // 收盘价
                ps.setDouble(7, fields.length > 9 ? parseDouble(fields[9]) : 0);  // 成交量
                ps.setDouble(8, fields.length > 10 ? parseDouble(fields[10]) : 0); // 成交额
                ps.addBatch();
                rows++;
            }

            return rows;
        }
    }

    private static String extractStockCode(String fileName) {
        // sh600519.csv → 600519, SZ000858.csv → 000858
        String name = fileName.replace(".csv", "").replace(".CSV", "");
        if (name.length() >= 2 && Character.isLetter(name.charAt(0))) {
            return name.substring(2);
        }
        return name;
    }

    private static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ======================== 分时数据导入 ========================

    /**
     * 导入指定年份目录下的分时 CSV 文件（GBK编码）。
     *
     * @param year 年份目录名，如 "2023"
     */
    public ImportResult importMinuteData(String year) throws IOException {
        Path dir = Paths.get(MINUTE_CSV_DIR, year);
        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException("分时数据目录不存在: " + dir);
        }

        List<Path> csvFiles;
        try (var stream = Files.list(dir)) {
            csvFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }

        if (csvFiles.isEmpty()) {
            System.out.println("  未找到CSV文件。");
            return new ImportResult(0, 0, 0);
        }

        System.out.println("  [" + year + "] 找到 " + csvFiles.size() + " 个分时CSV文件，开始导入...");

        dbManager.setBulkImportMode();

        int totalFiles = csvFiles.size();
        int successFiles = 0;
        int failedFiles = 0;
        long totalRows = 0;
        long startTime = System.currentTimeMillis();

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            String sql = "INSERT OR IGNORE INTO minute_kline " +
                         "(stock_code, date, time, open, high, low, close, volume, amount) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int batchCount = 0;

                for (int i = 0; i < csvFiles.size(); i++) {
                    Path file = csvFiles.get(i);
                    String fileName = file.getFileName().toString();

                    try {
                        long rows = importOneMinuteFile(file, ps);
                        batchCount += rows;
                        totalRows += rows;
                        successFiles++;

                        if (batchCount >= BATCH_SIZE) {
                            ps.executeBatch();
                            conn.commit();
                            batchCount = 0;
                        }
                    } catch (Exception e) {
                        failedFiles++;
                        System.err.println("    导入失败: " + fileName + " - " + e.getMessage());
                    }

                    if ((i + 1) % 100 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double pct = (i + 1) * 100.0 / totalFiles;
                        System.out.printf("  进度: %d/%d (%.1f%%), 已用时: %.1f秒, 已导入: %d行%n",
                                i + 1, totalFiles, pct, elapsed / 1000.0, totalRows);
                    }
                }

                if (batchCount > 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("数据库写入失败", e);
        } finally {
            dbManager.setNormalMode();
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        System.out.printf("%n  [%s] 导入完成！成功: %d 文件, 失败: %d 文件, 总行数: %d, 耗时: %d秒%n",
                year, successFiles, failedFiles, totalRows, elapsed);

        return new ImportResult(successFiles, failedFiles, totalRows);
    }

    private long importOneMinuteFile(Path file, PreparedStatement ps) throws Exception {
        String fileName = file.getFileName().toString();
        String stockCode = extractStockCode(fileName);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), GBK));
             CSVReader reader = new CSVReaderBuilder(br).build()) {

            // 跳过表头行
            String[] header = reader.readNext();
            if (header == null) {
                return 0;
            }

            long rows = 0;
            String[] fields;
            // GBK CSV 列: [0]=日期, [1]=时间, [2]=开盘, [3]=最高, [4]=最低, [5]=收盘, [6]=成交量, [7]=成交额
            while ((fields = reader.readNext()) != null) {
                if (fields.length < 8) continue;

                ps.setString(1, stockCode);
                ps.setString(2, fields[0]);            // 日期
                ps.setString(3, fields[1]);            // 时间
                ps.setDouble(4, parseDouble(fields[2])); // 开盘
                ps.setDouble(5, parseDouble(fields[3])); // 最高
                ps.setDouble(6, parseDouble(fields[4])); // 最低
                ps.setDouble(7, parseDouble(fields[5])); // 收盘
                ps.setDouble(8, parseDouble(fields[6])); // 成交量
                ps.setDouble(9, parseDouble(fields[7])); // 成交额
                ps.addBatch();
                rows++;
            }

            return rows;
        }
    }

    public record ImportResult(int successFiles, int failedFiles, long totalRows) {}
}
