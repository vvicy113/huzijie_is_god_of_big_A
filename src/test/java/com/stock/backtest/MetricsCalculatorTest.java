package com.stock.backtest;

import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.model.PerformanceMetrics;
import com.stock.model.TradeRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    @Test
    void shouldCalculateMetricsForProfitableTrades() {
        List<TradeRecord> trades = Arrays.asList(
                TradeRecord.builder()
                        .tradeIndex(1)
                        .buyDate(LocalDate.of(2024, 1, 2))
                        .buyPrice(100)
                        .buyShares(100)
                        .buyCommission(5)
                        .sellDate(LocalDate.of(2024, 1, 10))
                        .sellPrice(110)
                        .sellCommission(16)
                        .profit(11000 - 10000 - 5 - 16)
                        .profitPercent(9.79)
                        .holdingDays(8)
                        .build(),
                TradeRecord.builder()
                        .tradeIndex(2)
                        .buyDate(LocalDate.of(2024, 2, 1))
                        .buyPrice(110)
                        .buyShares(100)
                        .buyCommission(5)
                        .sellDate(LocalDate.of(2024, 2, 10))
                        .sellPrice(105)
                        .sellCommission(15.5)
                        .profit(10500 - 11000 - 5 - 15.5)
                        .profitPercent(-4.73)
                        .holdingDays(9)
                        .build()
        );

        List<Double> equity = new ArrayList<>();
        equity.add(100000.0);
        equity.add(101000.0);
        equity.add(102000.0);
        equity.add(101500.0);
        equity.add(102500.0);
        equity.add(103000.0);
        equity.add(102800.0);
        equity.add(103500.0);
        equity.add(104000.0);
        equity.add(103800.0);
        equity.add(104200.0);

        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < equity.size(); i++) {
            dates.add(LocalDate.of(2024, 1, 1).plusDays(i));
        }

        PerformanceMetrics metrics = MetricsCalculator.calculate(
                100000, 104200, trades, equity, dates, 0.02);

        assertEquals(2, metrics.getTotalTrades());
        assertEquals(1, metrics.getWinningTrades());
        assertEquals(1, metrics.getLosingTrades());
        assertEquals(50.0, metrics.getWinRate(), 1.0);
        assertTrue(metrics.getMaxDrawdown() >= 0);
        assertTrue(metrics.getTotalReturn() > 0);
    }

    @Test
    void shouldHandleEmptyTrades() {
        List<TradeRecord> trades = new ArrayList<>();
        List<Double> equity = Arrays.asList(100000.0, 100000.0);
        List<LocalDate> dates = Arrays.asList(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        PerformanceMetrics metrics = MetricsCalculator.calculate(
                100000, 100000, trades, equity, dates, 0.02);

        assertEquals(0, metrics.getTotalTrades());
        assertEquals(0.0, metrics.getTotalReturn());
    }
}
