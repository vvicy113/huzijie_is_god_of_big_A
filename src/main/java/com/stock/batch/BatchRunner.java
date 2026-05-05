package com.stock.batch;

import com.stock.backtest.engine.BacktestEngine;
import com.stock.backtest.strategy.ConfigurableStrategy;
import com.stock.config.BatchConfig;
import com.stock.config.BacktestConfig;
import com.stock.log.LogSetup;
import com.stock.model.BacktestResult;
import com.stock.model.PerformanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 批量回测运行器——遍历全部组合配置，逐个执行回测并记录结果。
 */
public class BatchRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchRunner.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<BatchConfig> configs;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final double initialCapital;
    private final Path outputDir;

    public BatchRunner(List<BatchConfig> configs, LocalDate dateFrom, LocalDate dateTo,
                       double initialCapital) {
        this.configs = configs;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.initialCapital = initialCapital;

        // 批量输出目录
        this.outputDir = Path.of("log", "batch_" + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }

    public void run() throws IOException {
        Files.createDirectories(outputDir);
        Path summaryFile = outputDir.resolve("summary.csv");

        // 写入 CSV 表头
        String header = "name,totalReturn,annualizedReturn,maxDrawdown,sharpeRatio,winRate,"
                + "totalTrades,wins,losses,avgProfit,avgLoss,profitLossRatio,finalCapital,"
                + "buyThreshold,maxPositions,cashRatio,stopLoss,takeProfit,maxHoldDays,replaceLower,allowAdd,"
                + "selectorCodes,scorerCodes,scorerWeights,time";
        Files.writeString(summaryFile, header + "\n");

        log.info("批量回测开始: {} 组  日期: {} ~ {}  初始资金: {}  输出: {}",
                configs.size(), dateFrom, dateTo, (int) initialCapital, outputDir);

        int done = 0;
        long startTime = System.currentTimeMillis();

        for (BatchConfig bc : configs) {
            try {
                BacktestResult result = runOne(bc);
                if (result != null) {
                    appendResult(summaryFile, bc, result);
                }
            } catch (Exception e) {
                log.error("  [FAIL] {} {}", bc.strategyName(), e.getMessage());
            }

            done++;
            if (done % 50 == 0 || done == configs.size()) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("  进度: {}/{} ({:.1f}%)  用时: {:.1f}分  预计剩余: {:.1f}分",
                        done, configs.size(), done * 100.0 / configs.size(),
                        elapsed / 60000.0,
                        (elapsed / 60000.0 / done) * (configs.size() - done));
            }
        }

        log.info("批量回测完成！总计: {} 组  输出目录: {}", configs.size(), outputDir);
    }

    private BacktestResult runOne(BatchConfig bc) {
        var strategy = new ConfigurableStrategy(bc.strategyName(),
                bc.selectorCodes(), bc.scorerCodes(), bc.scorerWeights(), bc.buyThreshold());

        BacktestConfig engineCfg = bc.toBacktestConfig(initialCapital);
        return new BacktestEngine(engineCfg).run(strategy, dateFrom, dateTo);
    }

    private void appendResult(Path summaryFile, BatchConfig bc, BacktestResult result) throws IOException {
        PerformanceMetrics m = result.getMetrics();
        String line = String.format("%s,%.2f,%.2f,%.2f,%.3f,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.0f,"
                        + "%.0f,%d,%.2f,%.0f,%.0f,%d,%s,%s,%s,%s,%s",
                bc.strategyName(),
                m.getTotalReturn(), m.getAnnualizedReturn(), m.getMaxDrawdown(),
                m.getSharpeRatio(), m.getWinRate(),
                m.getTotalTrades(), m.getWinningTrades(), m.getLosingTrades(),
                m.getAvgProfit(), m.getAvgLoss(), m.getProfitLossRatio(), m.getFinalCapital(),
                bc.buyThreshold(), bc.maxPositions(), bc.cashRatio(),
                bc.stopLossPct(), bc.takeProfitPct(), bc.maxHoldingDays(),
                bc.replaceLowerScored(), bc.allowAddPosition(),
                join(bc.selectorCodes()), join(bc.scorerCodes()), joinD(bc.scorerWeights()),
                LocalDateTime.now().format(TS));
        Files.writeString(summaryFile, line + "\n", java.nio.file.StandardOpenOption.APPEND);
    }

    private static String join(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append("-");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private static String joinD(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append("-");
            sb.append(String.format("%.2f", arr[i]));
        }
        return sb.toString();
    }
}
