package com.stock.analysis.fetcher;

import com.stock.model.ConceptInfo;
import com.stock.model.IndustryInfo;
import com.stock.model.IntradayTick;
import com.stock.model.StockInfo;

import java.util.List;

/**
 * 股票数据抓取接口。
 * <p>
 * 从外部数据源（如东方财富 API）获取股票的基本信息、行业、概念板块和分时数据。
 * 当前实现：{@link EastMoneyFetcher}（东方财富），备用：{@link SinaFinanceFetcher}（占位）。
 */
public interface StockDataFetcher {

    /**
     * 获取股票基本信息（名称、最新价、涨跌幅、市场）。
     *
     * @param stockCode 纯数字股票代码，如 "600519"
     * @return StockInfo，获取失败时 name 为 "未知"
     */
    StockInfo fetchBasicInfo(String stockCode);

    /**
     * 获取股票所属行业信息。
     *
     * @param stockCode 纯数字股票代码
     * @return IndustryInfo，含行业代码和行业名称
     */
    IndustryInfo fetchIndustry(String stockCode);

    /**
     * 获取股票所属的概念板块列表。
     *
     * @param stockCode 纯数字股票代码
     * @return 概念板块列表，获取失败返回空列表
     */
    List<ConceptInfo> fetchConcepts(String stockCode);

    /**
     * 获取股票当日分时数据（分钟级Tick）。
     * 仅在交易时段有数据返回，非交易时段返回空列表。
     *
     * @param stockCode 纯数字股票代码
     * @return 分时Tick列表，每个元素包含时间、价格、均价、成交量等
     */
    List<IntradayTick> fetchIntradayData(String stockCode);
}
