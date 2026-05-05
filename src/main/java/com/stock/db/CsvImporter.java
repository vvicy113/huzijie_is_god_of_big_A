package com.stock.db;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * 将 csv/日k/ 目录下的日K线 CSV 文件导入 SQLite。
 * 导入前先读取 股票列表.csv，只导入 sh/sz 开头且 market=主板 的 A 股主板股票。
 */
public class CsvImporter {

    private static final Logger log = LoggerFactory.getLogger(CsvImporter.class);
    private static final String CSV_DIR = "csv/日k";
    private static final String STOCK_LIST_FILE = "csv/股票列表.csv";
    private static final int BATCH_SIZE = 5000;

    private final DatabaseManager dbManager;
    private final Path csvDir;
    private final Path stockListFile;

    public CsvImporter() {
        this.dbManager = DatabaseManager.getInstance();
        this.csvDir = ProjectPaths.resolve(CSV_DIR);
        this.stockListFile = ProjectPaths.resolve(STOCK_LIST_FILE);
    }

    /**
     * 导入全部符合条件的日K线 CSV 文件。
     * 只导入 sh/sz 前缀 + 主板 的 A 股股票。
     *
     * @return 导入统计信息
     */
    public ImportResult importAll() throws IOException {
        // 1. 加载股票列表，筛选 sh/sz 主板股票
        Set<String> allowedCodes = loadMainBoardStocks();
        log.info("股票列表中共 {} 只 sh/sz 主板A股", allowedCodes.size());

        // 2. 扫描日k目录
        Path dir = csvDir;
        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException("日k目录不存在: " + dir);
        }

        List<Path> csvFiles;
        try (var stream = Files.list(dir)) {
            csvFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .toList();
        }

        // 3. 过滤：只处理 sh/sz 开头且在主板列表中的文件
        List<Path> filtered = new ArrayList<>();
        int skipped = 0;
        for (Path p : csvFiles) {
            String fileName = p.getFileName().toString();
            String prefix = fileName.substring(0, 2).toLowerCase();
            String code = extractStockCode(fileName);

            if ((prefix.equals("sh") || prefix.equals("sz")) && allowedCodes.contains(code)) {
                filtered.add(p);
            } else {
                skipped++;
            }
        }

        log.info("日k目录共 {} 个CSV文件", csvFiles.size());
        log.info("符合条件: {} 个 (sh/sz主板)，跳过: {} 个", filtered.size(), skipped);

        if (filtered.isEmpty()) {
            log.warn("没有找到符合条件的CSV文件");
            return new ImportResult(0, 0, 0);
        }

        log.info("开始导入...");

        int totalFiles = filtered.size();
        int successFiles = 0;
        int failedFiles = 0;
        long totalRows = 0;
        long startTime = System.currentTimeMillis();

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            DatabaseManager.setBulkImportMode(conn);
            conn.setAutoCommit(false);

            String sql = "INSERT OR IGNORE INTO daily_kline " +
                         "(stock_code, date, open, high, low, close, volume, amount) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int batchCount = 0;

                for (int i = 0; i < filtered.size(); i++) {
                    Path file = filtered.get(i);
                    String fileName = file.getFileName().toString();

                    try {
                        long rows = importOneFile(file, ps);
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
                        log.error("导入失败: {} - {}", fileName, e.getMessage());
                    }

                    if ((i + 1) % 100 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double pct = (i + 1) * 100.0 / totalFiles;
                        log.info("进度: {}/{} ({:.1f}%), 已用时: {:.1f}秒, 已导入: {}行",
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
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DatabaseManager.setNormalMode(conn);
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        log.info("导入完成！成功: {} 文件, 失败: {} 文件, 总行数: {}, 耗时: {}秒",
                successFiles, failedFiles, totalRows, elapsed);

        return new ImportResult(successFiles, failedFiles, totalRows);
    }

    /**
     * 从 股票列表.csv 加载 sh/sz 主板股票代码集合。
     */
    private Set<String> loadMainBoardStocks() throws IOException {
        Set<String> codes = new LinkedHashSet<>();
        if (!Files.exists(stockListFile)) {
            throw new FileNotFoundException("股票列表文件不存在: " + stockListFile);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(stockListFile.toFile()), StandardCharsets.UTF_8));
             CSVReader reader = new CSVReaderBuilder(br).build()) {

            String[] header = reader.readNext(); // skip header
            if (header == null) return codes;

            // columns: "code","symbol","name","area","industry","cnspell","market","list_date",...
            String[] fields;
            while ((fields = reader.readNext()) != null) {
                if (fields.length < 7) continue;

                String code = unquote(fields[0]);   // e.g. "000001.SZ"
                String name = unquote(fields[2]);   // e.g. "平安银行"
                String market = unquote(fields[6]); // e.g. "主板"

                if (!"主板".equals(market)) continue;
                if (name.toUpperCase().contains("ST")) continue;

                // 提取纯数字代码和交易所
                if (code.endsWith(".SH") || code.endsWith(".SZ")) {
                    String numCode = code.substring(0, 6);
                    codes.add(numCode);
                }
            }
        } catch (Exception e) {
            throw new IOException("读取股票列表失败", e);
        }
        return codes;
    }

    private long importOneFile(Path file, PreparedStatement ps) throws Exception {
        String fileName = file.getFileName().toString();
        String stockCode = extractStockCode(fileName);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8));
             CSVReader reader = new CSVReaderBuilder(br).build()) {

            String[] header = reader.readNext();
            if (header == null) return 0;

            long rows = 0;
            String[] fields;
            // CSV 列: [0]=股票代码, [1]=日期, [2]=开盘价, [3]=最高价, [4]=最低价,
            //          [5]=收盘价, [6]=昨收价, [7]=涨跌额, [8]=涨跌幅, [9]=成交量, [10]=成交额
            while ((fields = reader.readNext()) != null) {
                if (fields.length < 8) continue;

                ps.setString(1, stockCode);
                ps.setString(2, fields[1]);              // 日期
                ps.setDouble(3, parseDouble(fields[2])); // 开盘价
                ps.setDouble(4, parseDouble(fields[3])); // 最高价
                ps.setDouble(5, parseDouble(fields[4])); // 最低价
                ps.setDouble(6, parseDouble(fields[5])); // 收盘价
                ps.setDouble(7, fields.length > 9 ? parseDouble(fields[9]) : 0);   // 成交量
                ps.setDouble(8, fields.length > 10 ? parseDouble(fields[10]) : 0); // 成交额
                ps.addBatch();
                rows++;
            }

            return rows;
        }
    }

    private static String extractStockCode(String fileName) {
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

    private static String unquote(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    public record ImportResult(int successFiles, int failedFiles, long totalRows) {}
}
