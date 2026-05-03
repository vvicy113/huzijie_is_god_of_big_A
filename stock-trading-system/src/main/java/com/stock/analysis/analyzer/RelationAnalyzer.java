package com.stock.analysis.analyzer;

import com.stock.analysis.fetcher.EastMoneyFetcher;
import com.stock.analysis.fetcher.StockDataFetcher;
import com.stock.model.*;

import java.util.*;

public class RelationAnalyzer {

    private final StockDataFetcher fetcher;
    private final CorrelationCalculator correlationCalc;
    private final LeadLagDetector leadLagDetector;

    public RelationAnalyzer() {
        this.fetcher = new EastMoneyFetcher();
        this.correlationCalc = new CorrelationCalculator();
        this.leadLagDetector = new LeadLagDetector();
    }

    public RelationAnalyzer(StockDataFetcher fetcher) {
        this.fetcher = fetcher;
        this.correlationCalc = new CorrelationCalculator();
        this.leadLagDetector = new LeadLagDetector();
    }

    /**
     * 分析主股票与对比股票列表之间的关联关系。
     */
    public StockRelationReport analyze(String primaryCode, List<String> compareCodes) {
        // Step 1: 获取主股票数据
        StockInfo primary = fetcher.fetchBasicInfo(primaryCode);
        IndustryInfo primaryIndustry = fetcher.fetchIndustry(primaryCode);
        List<ConceptInfo> primaryConcepts = fetcher.fetchConcepts(primaryCode);
        List<IntradayTick> primaryIntraday = fetcher.fetchIntradayData(primaryCode);

        // Step 2: 获取对比股票数据
        List<StockInfo> comparedStocks = new ArrayList<>();
        Map<String, IndustryInfo> industryMap = new HashMap<>();
        Map<String, List<ConceptInfo>> conceptMap = new HashMap<>();
        Map<String, List<IntradayTick>> intradayMap = new HashMap<>();

        for (String code : compareCodes) {
            StockInfo info = fetcher.fetchBasicInfo(code);
            comparedStocks.add(info);
            industryMap.put(code, fetcher.fetchIndustry(code));
            conceptMap.put(code, fetcher.fetchConcepts(code));
            intradayMap.put(code, fetcher.fetchIntradayData(code));

            // API限速：每次请求间隔
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        }

        // Step 3: 行业匹配
        List<StockInfo> sameIndustry = new ArrayList<>();
        for (StockInfo s : comparedStocks) {
            IndustryInfo ind = industryMap.get(s.getCode());
            if (ind != null && ind.getIndustryName().equals(primaryIndustry.getIndustryName())) {
                sameIndustry.add(s);
            }
        }

        // Step 4: 概念重叠
        Set<String> primaryConceptNames = new HashSet<>();
        for (ConceptInfo c : primaryConcepts) {
            primaryConceptNames.add(c.getConceptName());
        }

        List<String> commonConcepts = new ArrayList<>();
        Map<String, List<StockInfo>> conceptDetail = new LinkedHashMap<>();

        for (String conceptName : primaryConceptNames) {
            List<StockInfo> stocksInConcept = new ArrayList<>();
            stocksInConcept.add(primary);
            for (StockInfo s : comparedStocks) {
                for (ConceptInfo ci : conceptMap.getOrDefault(s.getCode(), Collections.emptyList())) {
                    if (ci.getConceptName().equals(conceptName)) {
                        stocksInConcept.add(s);
                        break;
                    }
                }
            }
            if (stocksInConcept.size() > 1) {
                commonConcepts.add(conceptName);
                conceptDetail.put(conceptName, stocksInConcept);
            }
        }

        // Step 5: 价格相关性
        Map<String, Double> correlations = new LinkedHashMap<>();
        if (!primaryIntraday.isEmpty()) {
            for (StockInfo s : comparedStocks) {
                List<IntradayTick> compareTicks = intradayMap.get(s.getCode());
                if (compareTicks != null && !compareTicks.isEmpty()) {
                    double corr = correlationCalc.calculate(primaryIntraday, compareTicks);
                    correlations.put(s.getCode(), corr);
                }
            }
        }

        // Step 6: 领涨跟风
        Map<String, LeadLagResult> leadLagResults = new LinkedHashMap<>();
        if (!primaryIntraday.isEmpty()) {
            for (StockInfo s : comparedStocks) {
                List<IntradayTick> compareTicks = intradayMap.get(s.getCode());
                if (compareTicks != null && !compareTicks.isEmpty()) {
                    LeadLagResult result = leadLagDetector.detect(
                            primaryCode, s.getCode(), primaryIntraday, compareTicks, 10);
                    leadLagResults.put(s.getCode(), result);
                }
            }
        }

        // Step 7: 生成摘要
        String summary = buildSummary(primary, primaryIndustry, comparedStocks,
                sameIndustry, correlations, leadLagResults);

        return StockRelationReport.builder()
                .primaryStock(primary)
                .comparedStocks(comparedStocks)
                .sameIndustryStocks(sameIndustry)
                .primaryIndustry(primaryIndustry.getIndustryName())
                .commonConcepts(commonConcepts)
                .conceptDetail(conceptDetail)
                .priceCorrelation(correlations)
                .leadLagRelations(leadLagResults)
                .summary(summary)
                .build();
    }

    private String buildSummary(StockInfo primary, IndustryInfo industry,
                                 List<StockInfo> compared,
                                 List<StockInfo> sameIndustry,
                                 Map<String, Double> correlations,
                                 Map<String, LeadLagResult> leadLag) {
        StringBuilder sb = new StringBuilder();
        sb.append(primary.getName()).append("(").append(primary.getCode())
                .append(") 属于").append(industry.getIndustryName()).append("行业。\n");

        if (!sameIndustry.isEmpty()) {
            sb.append("与 ").append(sameIndustry.size()).append("只对比股票同属")
                    .append(industry.getIndustryName()).append("行业。");
        }

        // 找最高相关性
        double maxCorr = -1;
        String maxCorrCode = null;
        for (Map.Entry<String, Double> e : correlations.entrySet()) {
            if (e.getValue() > maxCorr) {
                maxCorr = e.getValue();
                maxCorrCode = e.getKey();
            }
        }
        if (maxCorrCode != null && maxCorr > 0.7) {
            sb.append(" 价格走势高度正相关（与").append(maxCorrCode).append("相关系数")
                    .append(String.format("%.3f", maxCorr)).append("）。");
        }

        // 领涨检测
        for (Map.Entry<String, LeadLagResult> e : leadLag.entrySet()) {
            LeadLagResult r = e.getValue();
            if (r.getLeadMinutes() > 0 && Math.abs(r.getConfidence()) > 0.4) {
                sb.append(r.getLeaderCode()).append("领先").append(r.getFollowerCode())
                        .append("约").append(r.getLeadMinutes()).append("分钟。");
            }
        }

        return sb.toString();
    }
}
