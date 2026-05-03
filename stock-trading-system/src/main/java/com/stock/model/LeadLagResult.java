package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LeadLagResult {
    private String leaderCode;
    private String followerCode;
    private int leadMinutes;
    private double confidence;
}
