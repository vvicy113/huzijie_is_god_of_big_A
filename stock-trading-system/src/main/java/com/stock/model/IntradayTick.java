package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class IntradayTick {
    private String time;
    private double price;
    private double avgPrice;
    private double volume;
    private double turnover;
    private double changePercent;
}
