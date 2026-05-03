package com.stock.backtest.loader;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.stock.db.KLineRepository;
import com.stock.model.KLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CsvDataLoader {

    private static final String DESKTOP_STOCKS_DIR =
            System.getProperty("user.home") + "/Desktop/stocks/日k";

    private final KLineRepository dbRepo;

    public CsvDataLoader() {
        this.dbRepo = new KLineRepository();
    }

    /**
     * 从指定文件路径加载K线数据（UTF-8编码），始终读取 CSV 文件。
     */
    public List<KLine> load(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            return parseKLine(reader);
        }
    }

    /**
     * 加载指定股票的K线数据。优先从 SQLite 数据库读取，数据库无数据时自动回退到
     * 桌面 stocks/日k 目录的 CSV 文件。
     *
     * 市场前缀规则：6xxxxx→sh, 0xxxxx/3xxxxx→sz, 4xxxxx/8xxxxx/9xxxxx→bj
     */
    public List<KLine> loadFromDesktopStocks(String stockCode) throws IOException {
        // 优先从数据库加载
        if (dbRepo.exists(stockCode)) {
            List<KLine> data = dbRepo.findByStockCode(stockCode);
            if (!data.isEmpty()) {
                return data;
            }
        }

        // 回退到 CSV 文件
        return loadFromCsvFile(stockCode);
    }

    private List<KLine> loadFromCsvFile(String stockCode) throws IOException {
        String prefix = detectMarketPrefix(stockCode);
        String fileName = prefix + stockCode + ".csv";
        Path filePath = Paths.get(DESKTOP_STOCKS_DIR, fileName);

        if (!Files.exists(filePath)) {
            String upperPrefix = prefix.toUpperCase();
            String upperFileName = upperPrefix + stockCode + ".csv";
            Path upperPath = Paths.get(DESKTOP_STOCKS_DIR, upperFileName);
            if (Files.exists(upperPath)) {
                filePath = upperPath;
            } else {
                throw new FileNotFoundException(
                        "未找到股票数据文件: " + fileName + " 或 " + upperFileName
                                + " (目录: " + DESKTOP_STOCKS_DIR + ")");
            }
        }

        return load(filePath.toString());
    }

    private List<KLine> parseKLine(BufferedReader reader) throws IOException {
        CsvToBean<KLine> csvToBean = new CsvToBeanBuilder<KLine>(reader)
                .withType(KLine.class)
                .withSkipLines(0)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        List<KLine> klines = csvToBean.parse();

        for (int i = 1; i < klines.size(); i++) {
            if (!klines.get(i).getDate().isAfter(klines.get(i - 1).getDate())) {
                throw new IllegalArgumentException(
                        "数据未按日期升序排列，问题在行号附近: " + (i + 1));
            }
        }

        for (KLine k : klines) {
            if (k.getDate() == null || k.getVolume() <= 0) {
                throw new IllegalArgumentException("数据不完整: " + k);
            }
        }

        return klines;
    }

    private static String detectMarketPrefix(String stockCode) {
        if (stockCode == null || stockCode.isEmpty()) {
            throw new IllegalArgumentException("股票代码不能为空");
        }
        char first = stockCode.charAt(0);
        switch (first) {
            case '6': return "sh";
            case '0':
            case '3': return "sz";
            case '4':
            case '8':
            case '9': return "bj";
            default:
                throw new IllegalArgumentException("无法识别的股票代码前缀: " + first);
        }
    }
}
