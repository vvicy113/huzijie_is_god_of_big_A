package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class StockRelationReport {
    private StockInfo primaryStock;
    private List<StockInfo> comparedStocks;
    private List<StockInfo> sameIndustryStocks;
    private String primaryIndustry;
    private List<String> commonConcepts;
    private Map<String, List<StockInfo>> conceptDetail;
    private Map<String, Double> priceCorrelation;
    private Map<String, LeadLagResult> leadLagRelations;
    private String summary;
}
