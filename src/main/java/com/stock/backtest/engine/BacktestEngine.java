package com.stock.backtest.engine;

import com.stock.backtest.loader.CsvDataLoader;
import com.stock.backtest.strategy.*;
import com.stock.db.KLineRepository;
import com.stock.model.*;
import com.stock.selector.StockSelector;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * 回测引擎 — 短线多股票回测（Dry Run 模式）。
 * <p>
 * 当前为最小实现：每天执行选股→打分→决策，展示信号板。
 * 真实买卖执行待后续实现。
 */
public class BacktestEngine {

    private final CsvDataLoader loader;
    private final KLineRepository repo;

    public BacktestEngine() {
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
    }

    /**
     * Dry run：遍历交易日，执行策略决策，打印每日信号。
     */
    public void dryRun(Strategy strategy, LocalDate from, LocalDate to) {
        strategy.onReset();
        StockSelector selector = strategy.getStockSelector();

        List<LocalDate> dates = getTradingDates(from, to);
        if (dates.isEmpty()) {
            System.out.println("  指定日期范围内无交易日数据。");
            return;
        }

        System.out.println("\n  策略: " + strategy.getName());
        System.out.println("  日期范围: " + from + " ~ " + to);
        System.out.println("  交易日数: " + dates.size());
        System.out.println();

        int totalBuy = 0, totalSell = 0;
        Map<String, Position> positions = new HashMap<>();

        for (int dayIdx = 0; dayIdx < dates.size(); dayIdx++) {
            LocalDate date = dates.get(dayIdx);
            List<String> candidates = selector.select(List.of(), date);
            if (candidates.isEmpty()) continue;

            // 加载候选K线（最近60日）
            Map<String, List<KLine>> klineMap = new LinkedHashMap<>();
            for (String code : candidates) {
                try {
                    List<KLine> klines = loader.loadDailyKLine(code, date.minusDays(60), date);
                    if (!klines.isEmpty()) klineMap.put(code, klines);
                } catch (IOException ignored) {}
            }

            if (klineMap.isEmpty()) continue;

            // 策略评估
            SignalBoard board = new SignalBoard();
            StrategyContext ctx = new StrategyContext(
                    candidates, klineMap, positions, 100_000, date, board);
            strategy.evaluate(ctx);

            if (board.isEmpty()) continue;

            int buyCount = 0, sellCount = 0;
            for (TradeSignal s : board.all()) {
                if (s.action() == TradeAction.BUY) buyCount++;
                else if (s.action() == TradeAction.SELL) sellCount++;
            }
            totalBuy += buyCount;
            totalSell += sellCount;

            // 打印每日 Top 5 信号
            System.out.printf("  %s  候选:%d  买入:%d  卖出:%d  持仓:%d%n",
                    date, candidates.size(), buyCount, sellCount, positions.size());
            var top = board.topN(5);
            for (TradeSignal s : top) {
                System.out.printf("    %-6s  %-4s  score=%.1f%n",
                        s.stockCode(), s.action(), s.score());
            }

            // 简易持仓更新（Dry run）
            for (TradeSignal s : board.all()) {
                if (s.action() == TradeAction.BUY) {
                    positions.put(s.stockCode(), Position.builder()
                            .holding(true).shares(100).avgCost(0)
                            .totalCost(0).currentValue(0).build());
                } else if (s.action() == TradeAction.SELL) {
                    positions.remove(s.stockCode());
                }
            }
        }

        System.out.printf("%n  总计: 买入信号 %d, 卖出信号 %d%n", totalBuy, totalSell);
    }

    private List<LocalDate> getTradingDates(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        List<String> allCodes = repo.getAllStockCodes();
        if (allCodes.isEmpty()) return dates;

        // 取任意一只股票的日期作为交易日历
        List<KLine> klines = repo.findByStockCode(allCodes.get(0), from, to);
        for (KLine k : klines) dates.add(k.getDate());
        return dates;
    }
}
