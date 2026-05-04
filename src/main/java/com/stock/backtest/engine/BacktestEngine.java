package com.stock.backtest.engine;

import com.stock.backtest.executor.DoubleRef;
import com.stock.backtest.executor.EqualSplitExecutor;
import com.stock.backtest.executor.TradeExecutor;
import com.stock.backtest.loader.CsvDataLoader;
import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.backtest.strategy.*;
import com.stock.db.KLineRepository;
import com.stock.model.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * 回测引擎——短线多股票回测。
 * <p>
 * 编排流程：选股→加载→评估→执行→记录。
 * TradeExecutor 可替换（默认等分资金）。
 */
public class BacktestEngine {

    private final CsvDataLoader loader;
    private final KLineRepository repo;
    private final TradeExecutor executor;

    public BacktestEngine() {
        this(new EqualSplitExecutor(5, 1.0));
    }

    public BacktestEngine(TradeExecutor executor) {
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
        this.executor = executor;
    }

    public void dryRun(Strategy strategy, LocalDate from, LocalDate to) {
        backtest(strategy, from, to, true);
    }

    public BacktestResult run(Strategy strategy, LocalDate from, LocalDate to, double initialCapital) {
        return backtest(strategy, from, to, false);
    }

    private BacktestResult backtest(Strategy strategy, LocalDate from, LocalDate to, boolean dry) {
        strategy.onReset();

        List<LocalDate> dates = getTradingDates(from, to);
        if (dates.isEmpty()) {
            System.out.println("  指定日期范围内无交易日数据。");
            return null;
        }

        double initialCapital = 100_000;
        DoubleRef cash = new DoubleRef(initialCapital);
        Map<String, Position> positions = new HashMap<>();
        List<TradeRecord> allTrades = new ArrayList<>();
        List<Double> equityValues = new ArrayList<>();
        List<LocalDate> equityDates = new ArrayList<>();

        System.out.println("\n  策略: " + strategy.getName());
        System.out.println("  执行器: " + executor.getName());
        System.out.println("  日期: " + from + " ~ " + to + " (" + dates.size() + "个交易日)");
        if (dry) System.out.println("  模式: Dry Run（不执行真实交易）");
        System.out.println();

        for (LocalDate date : dates) {
            // 1. 选股
            List<String> candidates = strategy.getStockSelector().select(List.of(), date);
            if (candidates.isEmpty()) continue;

            // 2. 加载K线 + 提取当日价格
            Map<String, List<KLine>> klineMap = new LinkedHashMap<>();
            Map<String, Double> prices = new LinkedHashMap<>();
            for (String code : candidates) {
                try {
                    List<KLine> klines = loader.loadDailyKLine(code, date.minusDays(60), date);
                    if (!klines.isEmpty()) {
                        klineMap.put(code, klines);
                        prices.put(code, klines.get(klines.size() - 1).getClose());
                    }
                } catch (IOException ignored) {}
            }
            if (klineMap.isEmpty()) continue;

            // 3. 策略评估
            SignalBoard board = new SignalBoard();
            StrategyContext ctx = new StrategyContext(candidates, klineMap, positions, cash.get(), date, board);
            strategy.evaluate(ctx);

            if (board.isEmpty()) {
                // 记录权益
                equityDates.add(date);
                equityValues.add(cash.get() + positions.values().stream()
                        .mapToDouble(p -> p.getShares() * p.getAvgCost()).sum());
                continue;
            }

            // 4. 执行交易
            List<TradeRecord> dayTrades;
            if (dry) {
                dayTrades = List.of();
                // Dry run: 仍更新持仓以展示信号
                for (TradeSignal s : board.all()) {
                    if (s.action() == TradeAction.BUY) positions.put(s.stockCode(),
                            Position.builder().holding(true).shares(100).avgCost(0).totalCost(0).currentValue(0).build());
                    else if (s.action() == TradeAction.SELL) positions.remove(s.stockCode());
                }
            } else {
                dayTrades = executor.execute(board, positions, cash, prices, date);
                allTrades.addAll(dayTrades);
            }

            // 5. 记录权益
            double totalEquity = cash.get();
            for (Position p : positions.values()) {
                // 按当日价格更新市值
                totalEquity += p.getShares() * p.getAvgCost(); // TODO: 用实际收盘价
            }
            equityDates.add(date);
            equityValues.add(totalEquity);

            // 打印每日
            int buys = 0, sells = 0;
            for (TradeSignal s : board.all()) {
                if (s.action() == TradeAction.BUY) buys++;
                if (s.action() == TradeAction.SELL) sells++;
            }
            System.out.printf("  %s  候选:%-4d  买:%-3d  卖:%-3d  持仓:%-2d  资金:%.0f%n",
                    date, candidates.size(), buys, sells, positions.size(), cash.get());
            if (!dry && !dayTrades.isEmpty()) {
                for (TradeRecord t : dayTrades) {
                    System.out.printf("    %-6s %-4s %d股 @%.2f%n",
                            t.getBuyDate() != null ? "BUY" : "SELL",
                            "", t.getBuyShares(), t.getBuyPrice());
                }
            }
        }

        // 收盘清仓
        if (!positions.isEmpty()) {
            // TODO: 按最后一日收盘价强制清仓
        }

        System.out.printf("%n  最终资金: %.0f  交易次数: %d  持仓: %d%n",
                cash.get(), allTrades.size(), positions.size());

        MetricsCalculator.calculate(initialCapital, cash.get(), allTrades, equityValues, equityDates);
        return null; // TODO: 返回完整 BacktestResult
    }

    private List<LocalDate> getTradingDates(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        List<String> allCodes = repo.getAllStockCodes();
        if (allCodes.isEmpty()) return dates;
        List<KLine> klines = repo.findByStockCode(allCodes.get(0), from, to);
        for (KLine k : klines) dates.add(k.getDate());
        return dates;
    }
}
