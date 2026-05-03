package com.stock.analysis.fetcher;

import com.stock.model.ConceptInfo;
import com.stock.model.IndustryInfo;
import com.stock.model.IntradayTick;
import com.stock.model.StockInfo;

import java.util.Collections;
import java.util.List;

/**
 * 新浪财经备用数据抓取器。当东方财富 API 失效时使用。
 * 当前为占位实现，返回空数据。
 */
public class SinaFinanceFetcher implements StockDataFetcher {

    @Override
    public StockInfo fetchBasicInfo(String stockCode) {
        return StockInfo.builder().code(stockCode).name("新浪暂未实现").build();
    }

    @Override
    public IndustryInfo fetchIndustry(String stockCode) {
        return IndustryInfo.builder().industryName("新浪暂未实现").build();
    }

    @Override
    public List<ConceptInfo> fetchConcepts(String stockCode) {
        return Collections.emptyList();
    }

    @Override
    public List<IntradayTick> fetchIntradayData(String stockCode) {
        return Collections.emptyList();
    }
}
