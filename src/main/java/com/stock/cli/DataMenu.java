package com.stock.cli;

import com.stock.config.RunConfig;
import com.stock.db.CsvImporter;
import com.stock.db.KLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DataMenu {

    private static final Logger log = LoggerFactory.getLogger(DataMenu.class);

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

    public void showStats() {
        KLineRepository repo = new KLineRepository();
        String dbPath = new File("data/stocks.db").getAbsolutePath();
        long fileSize = new File("data/stocks.db").length();
        log.info("数据库: {}  大小: {:.1f}MB  股票数: {}  总行数: {}",
                dbPath, fileSize / 1024.0 / 1024.0, repo.getStockCount(), repo.getTotalRows());
    }
}
