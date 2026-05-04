package com.stock.backtest.metrics;

import com.stock.model.PerformanceMetrics;
import com.stock.model.TradeRecord;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MetricsCalculator {

    public static PerformanceMetrics calculate(
            double initialCapital, double finalCapital,
            List<TradeRecord> trades,
            List<Double> equityCurve, List<LocalDate> dates,
            double riskFreeRate) {

        double totalReturn = (finalCapital - initialCapital) / initialCapital * 100;

        int totalDays = (int) (dates.get(dates.size() - 1).toEpochDay()
                - dates.get(0).toEpochDay());
        double years = Math.max(totalDays / 365.0, 0.01);
        double annualizedReturn = (Math.pow(1 + totalReturn / 100, 1.0 / years) - 1) * 100;

        double maxDrawdown = calculateMaxDrawdown(equityCurve);
        double sharpeRatio = calculateSharpeRatio(equityCurve, dates, riskFreeRate);

        int winningTrades = 0, losingTrades = 0;
        double totalWins = 0, totalLosses = 0;
        for (TradeRecord t : trades) {
            if (t.getSellDate() != null) {
                if (t.getProfit() >= 0) {
                    winningTrades++;
                    totalWins += t.getProfitPercent();
                } else {
                    losingTrades++;
                    totalLosses += Math.abs(t.getProfitPercent());
                }
            }
        }

        int totalTrades = winningTrades + losingTrades;
        double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;
        double avgProfit = winningTrades > 0 ? totalWins / winningTrades : 0;
        double avgLoss = losingTrades > 0 ? totalLosses / losingTrades : 0;
        double profitLossRatio = avgLoss > 0 ? avgProfit / avgLoss : 0;

        return PerformanceMetrics.builder()
                .totalReturn(totalReturn)
                .annualizedReturn(annualizedReturn)
                .maxDrawdown(maxDrawdown)
                .sharpeRatio(sharpeRatio)
                .winRate(winRate)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .avgProfit(avgProfit)
                .avgLoss(avgLoss)
                .profitLossRatio(profitLossRatio)
                .finalCapital(finalCapital)
                .initialCapital(initialCapital)
                .build();
    }

    private static double calculateMaxDrawdown(List<Double> equityCurve) {
        double peak = equityCurve.get(0);
        double maxDrawdown = 0;
        for (double value : equityCurve) {
            if (value > peak) {
                peak = value;
            }
            double drawdown = (peak - value) / peak * 100;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    private static double calculateSharpeRatio(List<Double> equityCurve,
                                                List<LocalDate> dates, double riskFreeRate) {
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            double prev = equityCurve.get(i - 1);
            if (prev > 0) {
                dailyReturns.add((equityCurve.get(i) - prev) / prev);
            }
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (double r : dailyReturns) {
            stats.addValue(r);
        }
        double meanReturn = stats.getMean();
        double stdDev = stats.getStandardDeviation();

        if (stdDev == 0) return 0;

        double dailyRiskFree = riskFreeRate / 252;
        return (meanReturn - dailyRiskFree) / stdDev * Math.sqrt(252);
    }
}
