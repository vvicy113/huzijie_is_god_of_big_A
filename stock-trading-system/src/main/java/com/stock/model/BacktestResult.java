package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class BacktestResult {
    private String stockCode;
    private String strategyName;
    private PerformanceMetrics metrics;
    private List<TradeRecord> trades;
    private List<LocalDate> equityCurveDates;
    private List<Double> equityCurveValues;
}
