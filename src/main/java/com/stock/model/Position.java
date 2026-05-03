package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Position {
    private boolean holding;
    private int shares;
    private double avgCost;
    private double totalCost;
    private double currentValue;

    public static Position empty() {
        return new Position(false, 0, 0.0, 0.0, 0.0);
    }
}
