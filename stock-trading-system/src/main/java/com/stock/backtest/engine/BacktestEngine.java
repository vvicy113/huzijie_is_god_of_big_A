package com.stock.backtest.engine;

import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.backtest.strategy.Strategy;
import com.stock.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BacktestEngine {

    private static final double COMMISSION_RATE = 0.00025;
    private static final double MIN_COMMISSION = 5.0;
    private static final double STAMP_TAX_RATE = 0.001;

    public BacktestResult run(Strategy strategy, List<KLine> klineData,
                              double initialCapital, String stockCode) {
        strategy.onReset();

        double capital = initialCapital;
        Position position = Position.empty();
        List<TradeRecord> trades = new ArrayList<>();
        int tradeCounter = 0;

        List<LocalDate> equityDates = new ArrayList<>();
        List<Double> equityValues = new ArrayList<>();

        for (int i = 0; i < klineData.size(); i++) {
            KLine kline = klineData.get(i);
            TradeAction signal = strategy.evaluate(i, klineData, position);

            if (signal == TradeAction.BUY && !position.isHolding()) {
                int sharesToBuy = (int) (capital / kline.getClose() / 100) * 100;
                if (sharesToBuy < 100) {
                    // 不够买1手，记录权益曲线后继续
                    double totalEquity = capital + (position.isHolding() ? position.getCurrentValue() : 0);
                    equityDates.add(kline.getDate());
                    equityValues.add(totalEquity);
                    continue;
                }

                double cost = sharesToBuy * kline.getClose();
                double commission = Math.max(cost * COMMISSION_RATE, MIN_COMMISSION);

                position = Position.builder()
                        .holding(true)
                        .shares(sharesToBuy)
                        .avgCost(kline.getClose())
                        .totalCost(cost + commission)
                        .currentValue(cost)
                        .build();
                capital -= (cost + commission);

                trades.add(TradeRecord.builder()
                        .tradeIndex(++tradeCounter)
                        .buyDate(kline.getDate())
                        .buyPrice(kline.getClose())
                        .buyShares(sharesToBuy)
                        .buyCommission(commission)
                        .build());

            } else if (signal == TradeAction.SELL && position.isHolding()) {
                double revenue = position.getShares() * kline.getClose();
                double commission = Math.max(revenue * COMMISSION_RATE, MIN_COMMISSION);
                double stampTax = revenue * STAMP_TAX_RATE;

                capital += (revenue - commission - stampTax);

                for (int j = trades.size() - 1; j >= 0; j--) {
                    TradeRecord t = trades.get(j);
                    if (t.getSellDate() == null) {
                        t.setSellDate(kline.getDate());
                        t.setSellPrice(kline.getClose());
                        t.setSellCommission(commission + stampTax);
                        double buyAmount = t.getBuyPrice() * t.getBuyShares();
                        double sellAmount = t.getSellPrice() * t.getBuyShares();
                        t.setProfit(sellAmount - buyAmount - t.getBuyCommission() - t.getSellCommission());
                        t.setProfitPercent(t.getProfit() / buyAmount * 100);
                        t.setHoldingDays((int) (t.getSellDate().toEpochDay() - t.getBuyDate().toEpochDay()));
                        break;
                    }
                }

                position = Position.empty();
            }

            if (position.isHolding()) {
                position.setCurrentValue(position.getShares() * kline.getClose());
            }

            double totalEquity = capital + (position.isHolding() ? position.getCurrentValue() : 0);
            equityDates.add(kline.getDate());
            equityValues.add(totalEquity);
        }

        if (position.isHolding()) {
            KLine lastKline = klineData.get(klineData.size() - 1);
            double revenue = position.getShares() * lastKline.getClose();
            double commission = Math.max(revenue * COMMISSION_RATE, MIN_COMMISSION);
            double stampTax = revenue * STAMP_TAX_RATE;
            capital += (revenue - commission - stampTax);

            for (int j = trades.size() - 1; j >= 0; j--) {
                TradeRecord t = trades.get(j);
                if (t.getSellDate() == null) {
                    t.setSellDate(lastKline.getDate());
                    t.setSellPrice(lastKline.getClose());
                    t.setSellCommission(commission + stampTax);
                    double buyAmount = t.getBuyPrice() * t.getBuyShares();
                    double sellAmount = t.getSellPrice() * t.getBuyShares();
                    t.setProfit(sellAmount - buyAmount - t.getBuyCommission() - t.getSellCommission());
                    t.setProfitPercent(t.getProfit() / buyAmount * 100);
                    t.setHoldingDays((int) (t.getSellDate().toEpochDay() - t.getBuyDate().toEpochDay()));
                    break;
                }
            }
        }

        PerformanceMetrics metrics = MetricsCalculator.calculate(
                initialCapital, capital, trades, equityValues, equityDates);

        return BacktestResult.builder()
                .stockCode(stockCode)
                .strategyName(strategy.getName())
                .metrics(metrics)
                .trades(trades)
                .equityCurveDates(equityDates)
                .equityCurveValues(equityValues)
                .build();
    }
}
