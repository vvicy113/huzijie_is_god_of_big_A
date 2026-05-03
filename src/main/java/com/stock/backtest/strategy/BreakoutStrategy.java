package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.util.List;

/**
 * N日突破策略：
 * - 收盘价创N日新高且未持仓 → BUY
 * - 收盘价创N日新低且持仓中 → SELL
 */
public class BreakoutStrategy implements Strategy {

    private final int period;

    public BreakoutStrategy(int period) {
        this.period = period;
    }

    @Override
    public String getName() {
        return "突破策略(" + period + "日)";
    }

    @Override
    public TradeAction evaluate(int currentIndex, List<KLine> history, Position currentPosition) {
        if (currentIndex < period) {
            return TradeAction.HOLD;
        }

        double currentClose = history.get(currentIndex).getClose();
        double highestHigh = history.get(currentIndex).getHigh();
        double lowestLow = history.get(currentIndex).getLow();

        for (int i = currentIndex - period; i < currentIndex; i++) {
            highestHigh = Math.max(highestHigh, history.get(i).getHigh());
            lowestLow = Math.min(lowestLow, history.get(i).getLow());
        }

        boolean newHigh = currentClose >= highestHigh;
        boolean newLow = currentClose <= lowestLow;

        if (newHigh && !currentPosition.isHolding()) {
            return TradeAction.BUY;
        }
        if (newLow && currentPosition.isHolding()) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }
}
