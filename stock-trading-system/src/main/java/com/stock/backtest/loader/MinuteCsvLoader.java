package com.stock.backtest.loader;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.stock.db.KLineRepository;
import com.stock.model.MinuteData;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 分时数据加载器。读取 GBK 编码的分钟级 CSV 文件。
 * 支持从项目 csv/ 目录按年份和股票代码自动定位文件。
 */
public class MinuteCsvLoader {

    private static final Charset GBK = Charset.forName("GBK");
    private static final String CSV_DIR = "csv";

    private final KLineRepository dbRepo;

    public MinuteCsvLoader() {
        this.dbRepo = new KLineRepository();
    }

    /**
     * 从指定文件路径加载分时数据（GBK编码）。
     */
    public List<MinuteData> load(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), GBK))) {
            return parseMinuteCsv(reader);
        }
    }

    /**
     * 从 csv/{year}/{prefix}{code}.csv 加载分时数据。
     * 优先从数据库读取，无数据时读取 CSV 文件。
     */
    public List<MinuteData> loadFromCsvDir(String stockCode, String year) throws IOException {
        if (dbRepo.existsMinute(stockCode)) {
            List<MinuteData> data = dbRepo.findMinuteByStockCode(stockCode);
            if (!data.isEmpty()) {
                return data;
            }
        }
        return loadFromCsvFile(stockCode, year);
    }

    private List<MinuteData> loadFromCsvFile(String stockCode, String year) throws IOException {
        String prefix = detectMarketPrefix(stockCode);
        String fileName = prefix + stockCode + ".csv";
        Path filePath = Paths.get(CSV_DIR, year, fileName);

        if (!Files.exists(filePath)) {
            String upperPrefix = prefix.toUpperCase();
            String upperFileName = upperPrefix + stockCode + ".csv";
            Path upperPath = Paths.get(CSV_DIR, year, upperFileName);
            if (Files.exists(upperPath)) {
                filePath = upperPath;
            } else {
                throw new FileNotFoundException(
                        "未找到分时数据文件: " + fileName + " 或 " + upperFileName
                                + " (目录: " + CSV_DIR + "/" + year + ")");
            }
        }

        return load(filePath.toString());
    }

    private List<MinuteData> parseMinuteCsv(BufferedReader reader) throws IOException {
        CsvToBean<MinuteData> csvToBean = new CsvToBeanBuilder<MinuteData>(reader)
                .withType(MinuteData.class)
                .withSkipLines(0)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        List<MinuteData> data = csvToBean.parse();

        for (MinuteData d : data) {
            if (d.getDate() == null || d.getTime() == null) {
                throw new IllegalArgumentException("数据不完整: " + d);
            }
        }

        return data;
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
