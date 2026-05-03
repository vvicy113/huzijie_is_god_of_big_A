package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.util.List;

/**
 * MACD金叉死叉策略。
 * DIF上穿DEA买入，DIF下穿DEA卖出。
 */
public class GoldenCrossStrategy implements Strategy {

    private final int fast;
    private final int slow;
    private final int signal;

    public GoldenCrossStrategy() {
        this(12, 26, 9);
    }

    public GoldenCrossStrategy(int fast, int slow, int signal) {
        this.fast = fast;
        this.slow = slow;
        this.signal = signal;
    }

    @Override
    public String getName() {
        return "MACD金叉死叉(" + fast + "," + slow + "," + signal + ")";
    }

    @Override
    public TradeAction evaluate(int currentIndex, List<KLine> history, Position currentPosition) {
        if (currentIndex < slow + signal) {
            return TradeAction.HOLD;
        }

        double[] dif = calcDif(history, currentIndex);
        double deaNow = calcDea(dif, currentIndex);
        double deaPrev = calcDea(dif, currentIndex - 1);
        double difNow = dif[currentIndex];
        double difPrev = dif[currentIndex - 1];

        boolean goldenCross = difPrev <= deaPrev && difNow > deaNow;
        boolean deathCross = difPrev >= deaPrev && difNow < deaNow;

        if (goldenCross && !currentPosition.isHolding()) {
            return TradeAction.BUY;
        }
        if (deathCross && currentPosition.isHolding()) {
            return TradeAction.SELL;
        }
        return TradeAction.HOLD;
    }

    private double[] calcDif(List<KLine> history, int endIndex) {
        double[] dif = new double[history.size()];
        double fastEma = history.get(0).getClose();
        double slowEma = history.get(0).getClose();
        double fastAlpha = 2.0 / (fast + 1);
        double slowAlpha = 2.0 / (slow + 1);

        for (int i = 0; i <= endIndex; i++) {
            double close = history.get(i).getClose();
            fastEma = close * fastAlpha + fastEma * (1 - fastAlpha);
            slowEma = close * slowAlpha + slowEma * (1 - slowAlpha);
            dif[i] = fastEma - slowEma;
        }
        return dif;
    }

    private double calcDea(double[] dif, int endIndex) {
        double dea = dif[0];
        double alpha = 2.0 / (signal + 1);
        for (int i = 1; i <= endIndex; i++) {
            dea = dif[i] * alpha + dea * (1 - alpha);
        }
        return dea;
    }
}
