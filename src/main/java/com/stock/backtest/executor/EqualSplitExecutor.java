package com.stock.backtest.executor;

import com.stock.backtest.strategy.SignalBoard;
import com.stock.config.BacktestConfig;
import com.stock.model.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 等分资金执行器。
 * <p>
 * 交易规则：
 * <ol>
 *   <li>先处理 SELL 信号——清仓，释放资金</li>
 *   <li>再处理 BUY 信号——按分数从高到低，等分可用资金买入</li>
 *   <li>已达持仓上限则跳过后续买入（可配 replaceLowerScored 卖低换高）</li>
 *   <li>资金不足 1 手（100股）则跳过</li>
 * </ol>
 */
public class EqualSplitExecutor implements TradeExecutor {

    private final BacktestConfig config;
    private int tradeCounter;

    public EqualSplitExecutor(BacktestConfig config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return "等分资金(max=" + config.maxPositions() + ")";
    }

    @Override
    public List<TradeRecord> execute(
            SignalBoard board,
            Map<String, Position> positions,
            DoubleRef cash,
            Map<String, Double> prices,
            LocalDate date) {

        List<TradeRecord> records = new ArrayList<>();
        double commRate = config.commissionRate();
        double minComm = config.minCommission();
        double taxRate = config.stampTaxRate();

        // 1. 先卖
        for (TradeSignal s : board.all()) {
            if (s.action() != TradeAction.SELL) continue;
            Position pos = positions.remove(s.stockCode());
            if (pos == null || !pos.isHolding()) continue;

            Double price = prices.get(s.stockCode());
            if (price == null || price <= 0) continue;

            double revenue = pos.getShares() * price;
            double commission = Math.max(revenue * commRate, minComm);
            double stampTax = revenue * taxRate;
            double net = revenue - commission - stampTax;
            cash.add(net);

            records.add(TradeRecord.builder()
                    .tradeIndex(++tradeCounter)
                    .buyPrice(pos.getAvgCost()).buyShares(pos.getShares())
                    .sellDate(date).sellPrice(price)
                    .sellCommission(commission + stampTax)
                    .profit(net - pos.getTotalCost())
                    .profitPercent((net - pos.getTotalCost()) / pos.getTotalCost() * 100)
                    .holdingDays(0).build());
        }

        // 2. 收集 BUY 候选
        List<TradeSignal> buyCandidates = new ArrayList<>();
        for (TradeSignal s : board.all()) {
            if (s.action() != TradeAction.BUY) continue;
            if (positions.containsKey(s.stockCode()) && !config.allowAddPosition()) continue;
            buyCandidates.add(s);
        }
        if (buyCandidates.isEmpty()) return records;

        // 3. 满仓替换
        int slots = config.maxPositions() - positions.size();
        if (slots <= 0) {
            if (!config.replaceLowerScored()) return records;
            // 卖最低分换新高分（简化：只换第一名）
            TradeSignal best = buyCandidates.get(0);
            TradeSignal worst = null;
            double worstScore = Double.MAX_VALUE;
            for (TradeSignal s : board.all()) {
                if (positions.containsKey(s.stockCode()) && s.score() < worstScore) {
                    worstScore = s.score();
                    worst = s;
                }
            }
            if (worst == null || worst.score() >= best.score()) return records;
            // 卖旧
            Position old = positions.remove(worst.stockCode());
            Double price = prices.get(worst.stockCode());
            if (old != null && price != null) {
                double rev = old.getShares() * price;
                cash.add(rev - Math.max(rev * commRate, minComm) - rev * taxRate);
            }
            slots = 1;
        }

        // 4. 等分买入
        int buyCount = Math.min(buyCandidates.size(), slots);
        if (buyCount <= 0) return records;

        double totalCash = cash.get() * config.cashRatio();
        double perStock = totalCash / buyCount;

        for (int i = 0; i < buyCount; i++) {
            TradeSignal s = buyCandidates.get(i);
            Double price = prices.get(s.stockCode());
            if (price == null || price <= 0) continue;

            int shares = (int) (perStock / price / 100) * 100;
            if (shares < 100) continue;

            double cost = shares * price;
            double commission = Math.max(cost * commRate, minComm);
            if (cost + commission > cash.get()) continue;

            cash.subtract(cost + commission);
            positions.put(s.stockCode(), Position.builder()
                    .holding(true).shares(shares).avgCost(price)
                    .totalCost(cost + commission).currentValue(cost).build());

            records.add(TradeRecord.builder()
                    .tradeIndex(++tradeCounter)
                    .buyDate(date).buyPrice(price).buyShares(shares)
                    .buyCommission(commission).build());
        }

        return records;
    }
}
