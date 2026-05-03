package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.util.List;

/**
 * 均线交叉策略：短期均线上穿长期均线买入，下穿卖出。
 */
public class MovingAverageCrossStrategy implements Strategy {

    private final int shortWindow;
    private final int longWindow;

    public MovingAverageCrossStrategy(int shortWindow, int longWindow) {
        if (shortWindow >= longWindow) {
            throw new IllegalArgumentException("短期窗口必须小于长期窗口");
        }
        this.shortWindow = shortWindow;
        this.longWindow = longWindow;
    }

    @Override
    public String getName() {
        return "均线交叉(" + shortWindow + "," + longWindow + ")";
    }

    @Override
    public TradeAction evaluate(int currentIndex, List<KLine> history, Position currentPosition) {
        if (currentIndex < longWindow) {
            return TradeAction.HOLD;
        }

        double shortMaNow = calculateMa(history, shortWindow, currentIndex);
        double longMaNow = calculateMa(history, longWindow, currentIndex);
        double shortMaPrev = calculateMa(history, shortWindow, currentIndex - 1);
        double longMaPrev = calculateMa(history, longWindow, currentIndex - 1);

        boolean goldenCross = shortMaPrev <= longMaPrev && shortMaNow > longMaNow;
        boolean deathCross = shortMaPrev >= longMaPrev && shortMaNow < longMaNow;

        if (goldenCross && !currentPosition.isHolding()) {
            return TradeAction.BUY;
        }
        if (deathCross && currentPosition.isHolding()) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }

    private double calculateMa(List<KLine> data, int window, int endIndex) {
        double sum = 0;
        for (int i = endIndex - window + 1; i <= endIndex; i++) {
            sum += data.get(i).getClose();
        }
        return sum / window;
    }
}
