package com.stock.backtest.engine;

import com.stock.backtest.executor.DoubleRef;
import com.stock.backtest.executor.EqualSplitExecutor;
import com.stock.backtest.executor.TradeExecutor;
import com.stock.backtest.loader.CsvDataLoader;
import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.backtest.strategy.*;
import com.stock.config.BacktestConfig;
import com.stock.db.KLineRepository;
import com.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);

    private final CsvDataLoader loader;
    private final KLineRepository repo;
    private final TradeExecutor executor;
    private final BacktestConfig config;

    public BacktestEngine() {
        this(BacktestConfig.defaults());
    }

    public BacktestEngine(BacktestConfig config) {
        this.config = config;
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
        this.executor = new EqualSplitExecutor(config);
    }

    public BacktestEngine(BacktestConfig config, TradeExecutor executor) {
        this.config = config;
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
        this.executor = executor;
    }

    public void dryRun(Strategy strategy, LocalDate from, LocalDate to) {
        backtest(strategy, from, to, true);
    }

    public BacktestResult run(Strategy strategy, LocalDate from, LocalDate to) {
        return backtest(strategy, from, to, false);
    }

    private BacktestResult backtest(Strategy strategy, LocalDate from, LocalDate to, boolean dry) {
        strategy.onReset();

        List<LocalDate> dates = getTradingDates(from, to);
        if (dates.isEmpty()) {
            log.warn("指定日期范围内无交易日数据");
            return null;
        }

        DoubleRef cash = new DoubleRef(config.initialCapital());
        Map<String, Position> positions = new HashMap<>();
        Map<String, LocalDate> buyDates = new HashMap<>();
        Map<String, Double> lastPrices = new HashMap<>();
        List<TradeRecord> allTrades = new ArrayList<>();
        List<Double> equityValues = new ArrayList<>();
        List<LocalDate> equityDates = new ArrayList<>();

        log.info("策略: {}  执行器: {}  日期: {} ~ {} ({}个交易日)",
                strategy.getName(), executor.getName(), from, to, dates.size());

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

            // 4. 风控检查：止损/止盈/持仓天数
            applyRiskCheck(positions, buyDates, prices, date, board);

            if (board.isEmpty()) {
                equityDates.add(date);
                equityValues.add(cash.get() + positions.values().stream()
                        .mapToDouble(p -> p.getShares() * p.getAvgCost()).sum());
                continue;
            }

            // 5. 执行交易
            List<TradeRecord> dayTrades;
            if (dry) {
                dayTrades = List.of();
                // Dry run: 仍更新持仓以展示信号
                for (TradeSignal s : board.all()) {
                    if (s.action() == TradeAction.BUY) {
                        positions.put(s.stockCode(),
                                Position.builder().holding(true).shares(100).avgCost(0).totalCost(0).currentValue(0).build());
                        buyDates.put(s.stockCode(), date);
                    } else if (s.action() == TradeAction.SELL) {
                        positions.remove(s.stockCode());
                        buyDates.remove(s.stockCode());
                    }
                }
            } else {
                dayTrades = executor.execute(board, positions, cash, prices, date);
                allTrades.addAll(dayTrades);
                for (TradeSignal s : board.all()) {
                    if (s.action() == TradeAction.BUY) buyDates.put(s.stockCode(), date);
                    else if (s.action() == TradeAction.SELL) buyDates.remove(s.stockCode());
                }
            }

            // 5. 记录权益
            double totalEquity = cash.get();
            for (var entry : positions.entrySet()) {
                String code = entry.getKey();
                Position p = entry.getValue();
                Double close = prices.get(code);
                totalEquity += p.getShares() * (close != null ? close : p.getAvgCost());
            }
            equityDates.add(date);
            equityValues.add(totalEquity);

            // 打印每日
            int buys = 0, sells = 0;
            for (TradeSignal s : board.all()) {
                if (s.action() == TradeAction.BUY) buys++;
                if (s.action() == TradeAction.SELL) sells++;
            }
            log.info("{}  候选:{}  买:{}  卖:{}  持仓:{}  资金:{}",
                    date, candidates.size(), buys, sells, positions.size(), (int) cash.get());
            lastPrices = new HashMap<>(prices);
        }

        // 收盘强制清仓
        if (!positions.isEmpty() && config.forceCloseAtEnd() && !lastPrices.isEmpty()) {
            double commRate = config.commissionRate();
            double minComm = config.minCommission();
            double taxRate = config.stampTaxRate();
            for (var it = positions.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                String code = entry.getKey();
                Position p = entry.getValue();
                Double close = lastPrices.get(code);
                if (close == null) close = p.getAvgCost();
                double revenue = p.getShares() * close;
                double commission = Math.max(revenue * commRate, minComm);
                double stampTax = revenue * taxRate;
                cash.add(revenue - commission - stampTax);
                allTrades.add(TradeRecord.builder()
                        .tradeIndex(allTrades.size() + 1)
                        .buyDate(null).buyPrice(p.getAvgCost()).buyShares(p.getShares())
                        .sellDate(dates.get(dates.size() - 1)).sellPrice(close)
                        .sellCommission(commission + stampTax)
                        .profit(revenue - commission - stampTax - p.getTotalCost()).build());
                it.remove();
            }
        }

        log.info("最终资金: {}  交易次数: {}", (int) cash.get(), allTrades.size());

        var metrics = MetricsCalculator.calculate(config.initialCapital(), cash.get(),
                allTrades, equityValues, equityDates, config.riskFreeRate());

        return BacktestResult.builder()
                .stockCode("多股票").strategyName(strategy.getName())
                .metrics(metrics).trades(allTrades)
                .equityCurveDates(equityDates).equityCurveValues(equityValues)
                .build();
    }

    /** 风控检查：遍历持仓，对触发止损/止盈/超期的股票添加 SELL 信号 */
    private void applyRiskCheck(Map<String, Position> positions, Map<String, LocalDate> buyDates,
                                Map<String, Double> prices, LocalDate date, SignalBoard board) {
        if (!config.stopLossEnabled() && !config.takeProfitEnabled() && !config.maxHoldingDaysEnabled())
            return;

        for (var entry : positions.entrySet()) {
            String code = entry.getKey();
            Position pos = entry.getValue();
            Double close = prices.get(code);
            if (close == null || close <= 0) continue;

            LocalDate buyDate = buyDates.get(code);
            boolean sell = false;

            // 止损
            if (config.stopLossEnabled()) {
                double loss = (pos.getAvgCost() - close) / pos.getAvgCost() * 100;
                if (loss >= config.stopLossPct()) sell = true;
            }
            // 止盈
            if (!sell && config.takeProfitEnabled()) {
                double profit = (close - pos.getAvgCost()) / pos.getAvgCost() * 100;
                if (profit >= config.takeProfitPct()) sell = true;
            }
            // 持仓天数
            if (!sell && config.maxHoldingDaysEnabled() && buyDate != null) {
                long days = date.toEpochDay() - buyDate.toEpochDay();
                if (days >= config.maxHoldingDays()) sell = true;
            }

            if (sell) {
                board.add(new TradeSignal(code, TradeAction.SELL, -1));
            }
        }
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
