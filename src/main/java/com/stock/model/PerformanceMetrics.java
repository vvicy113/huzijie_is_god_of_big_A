package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PerformanceMetrics {
    private double totalReturn;
    private double annualizedReturn;
    private double maxDrawdown;
    private double sharpeRatio;
    private double winRate;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private double avgProfit;
    private double avgLoss;
    private double profitLossRatio;
    private double finalCapital;
    private double initialCapital;
}
