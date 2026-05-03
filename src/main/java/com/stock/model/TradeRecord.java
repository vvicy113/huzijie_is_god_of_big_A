package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class TradeRecord {
    private int tradeIndex;
    private LocalDate buyDate;
    private double buyPrice;
    private int buyShares;
    private double buyCommission;
    private LocalDate sellDate;
    private double sellPrice;
    private double sellCommission;
    private double profit;
    private double profitPercent;
    private int holdingDays;
}
