package com.stock.analysis.fetcher;

import com.stock.model.ConceptInfo;
import com.stock.model.IndustryInfo;
import com.stock.model.IntradayTick;
import com.stock.model.StockInfo;

import java.util.List;

public interface StockDataFetcher {
    StockInfo fetchBasicInfo(String stockCode);
    IndustryInfo fetchIndustry(String stockCode);
    List<ConceptInfo> fetchConcepts(String stockCode);
    List<IntradayTick> fetchIntradayData(String stockCode);
}
