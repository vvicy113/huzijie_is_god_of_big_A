package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class StockInfo {
    private String code;
    private String name;
    private String market;
    private String fullCode;
    private double currentPrice;
    private double changePercent;
}
