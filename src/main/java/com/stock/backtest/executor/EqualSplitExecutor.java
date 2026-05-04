package com.stock.backtest.executor;

import com.stock.backtest.strategy.SignalBoard;
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
 *   <li>已达持仓上限则跳过后续买入</li>
 *   <li>资金不足 1 手（100股）则跳过</li>
 * </ol>
 */
public class EqualSplitExecutor implements TradeExecutor {

    private static final double COMMISSION_RATE = 0.00025;
    private static final double MIN_COMMISSION = 5.0;
    private static final double STAMP_TAX_RATE = 0.001;

    private final int maxPositions;
    private final double cashRatio;
    private int tradeCounter;

    public EqualSplitExecutor(int maxPositions, double cashRatio) {
        if (maxPositions < 1) throw new IllegalArgumentException("maxPositions 必须 >= 1");
        if (cashRatio <= 0 || cashRatio > 1) throw new IllegalArgumentException("cashRatio 必须在 (0,1]");
        this.maxPositions = maxPositions;
        this.cashRatio = cashRatio;
    }

    @Override
    public String getName() {
        return "等分资金(max=" + maxPositions + ")";
    }

    @Override
    public List<TradeRecord> execute(
            SignalBoard board,
            Map<String, Position> positions,
            DoubleRef cash,
            Map<String, Double> prices,
            LocalDate date) {

        List<TradeRecord> records = new ArrayList<>();

        // 1. 先卖
        for (TradeSignal s : board.all()) {
            if (s.action() != TradeAction.SELL) continue;
            Position pos = positions.remove(s.stockCode());
            if (pos == null || !pos.isHolding()) continue;

            Double price = prices.get(s.stockCode());
            if (price == null || price <= 0) continue;

            double revenue = pos.getShares() * price;
            double commission = Math.max(revenue * COMMISSION_RATE, MIN_COMMISSION);
            double stampTax = revenue * STAMP_TAX_RATE;
            double net = revenue - commission - stampTax;
            cash.add(net);

            records.add(TradeRecord.builder()
                    .tradeIndex(++tradeCounter)
                    .buyDate(null).buyPrice(pos.getAvgCost())
                    .buyShares(pos.getShares()).buyCommission(pos.getTotalCost() - pos.getShares() * pos.getAvgCost())
                    .sellDate(date).sellPrice(price)
                    .sellCommission(commission + stampTax)
                    .profit(net - pos.getTotalCost())
                    .profitPercent((net - pos.getTotalCost()) / pos.getTotalCost() * 100)
                    .holdingDays(0).build());
        }

        // 2. 收集 BUY 候选（排除已持仓）
        List<TradeSignal> buyCandidates = new ArrayList<>();
        for (TradeSignal s : board.all()) {
            if (s.action() == TradeAction.BUY && !positions.containsKey(s.stockCode())) {
                buyCandidates.add(s);
            }
        }
        if (buyCandidates.isEmpty()) return records;

        // 3. 等分资金买入
        int slots = maxPositions - positions.size();
        int buyCount = Math.min(buyCandidates.size(), slots);
        if (buyCount <= 0) return records;

        double totalCash = cash.get() * cashRatio;
        double perStock = totalCash / buyCount;

        for (int i = 0; i < buyCount; i++) {
            TradeSignal s = buyCandidates.get(i);
            Double price = prices.get(s.stockCode());
            if (price == null || price <= 0) continue;

            int shares = (int) (perStock / price / 100) * 100;
            if (shares < 100) continue;

            double cost = shares * price;
            double commission = Math.max(cost * COMMISSION_RATE, MIN_COMMISSION);
            if (cost + commission > cash.get()) continue;

            cash.subtract(cost + commission);

            positions.put(s.stockCode(), Position.builder()
                    .holding(true).shares(shares).avgCost(price)
                    .totalCost(cost + commission).currentValue(cost).build());

            records.add(TradeRecord.builder()
                    .tradeIndex(++tradeCounter)
                    .buyDate(date).buyPrice(price)
                    .buyShares(shares).buyCommission(commission)
                    .build());
        }

        return records;
    }
}
