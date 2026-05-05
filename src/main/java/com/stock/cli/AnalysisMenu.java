package com.stock.cli;

import com.stock.analysis.analyzer.RelationAnalyzer;
import com.stock.analysis.llm.LlmConfig;
import com.stock.analysis.llm.LlmReportService;
import com.stock.config.RunConfig;
import com.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AnalysisMenu {

    private static final Logger log = LoggerFactory.getLogger(AnalysisMenu.class);
    private final RelationAnalyzer analyzer = new RelationAnalyzer();
    private final LlmReportService llmService = new LlmReportService();

    public void runAuto(RunConfig config) {
        String primary = config.primaryCode();
        String compares = config.compareCodes();
        if (primary.isEmpty() || compares.isEmpty()) {
            log.warn("分析模式需要配置 analysis.primaryCode 和 analysis.compareCodes");
            return;
        }
        List<String> compareCodes = Arrays.stream(compares.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        log.info("正在联网抓取数据...");
        StockRelationReport report = analyzer.analyze(primary, compareCodes);
        printReport(report);
        if (LlmConfig.isConfigured()) {
            String llm = llmService.analyze(report);
            if (llm != null) log.info("[AI 综合分析]\n  {}", llm.trim());
        }
    }

    private void printReport(StockRelationReport report) {
        int w = 56;
        String sep = "─".repeat(w);
        StockInfo primary = report.getPrimaryStock();

        log.info("{}", sep);
        log.info("  股票关联分析报告");
        log.info("{}", sep);
        log.info("  主股票: {}({})  {}", primary.getName(), primary.getCode(), String.format("%.2f元", primary.getCurrentPrice()));
        log.info("{}", sep);
        log.info("  [行业板块]");
        log.info("  主股票行业: {}", report.getPrimaryIndustry());
        if (!report.getSameIndustryStocks().isEmpty()) {
            StringBuilder sb = new StringBuilder("  同行业股票: ");
            for (StockInfo s : report.getSameIndustryStocks()) sb.append(s.getName()).append("(").append(s.getCode()).append(") ");
            log.info("{}", sb);
        } else {
            log.info("  无同行业股票。");
        }

        log.info("{}", sep);
        log.info("  [概念/题材重叠]");
        if (!report.getCommonConcepts().isEmpty()) {
            log.info("  共同概念: {}", String.join(", ", report.getCommonConcepts()));
            if (report.getConceptDetail() != null) {
                for (var e : report.getConceptDetail().entrySet()) {
                    List<String> names = e.getValue().stream().map(s -> s.getName() + "(" + s.getCode() + ")").toList();
                    log.info("    - \"{}\": {}", e.getKey(), String.join(", ", names));
                }
            }
        } else {
            log.info("  无共同概念板块。");
        }

        log.info("{}", sep);
        log.info("  [价格相关性 (分时数据Pearson系数)]");
        if (!report.getPriceCorrelation().isEmpty()) {
            for (var e : report.getPriceCorrelation().entrySet()) {
                String desc = Math.abs(e.getValue()) > 0.7 ? "高度相关" : Math.abs(e.getValue()) > 0.4 ? "中度相关" : "弱相关";
                log.info("  {} vs {}: {:+.3f} ({})", primary.getCode(), e.getKey(), e.getValue(), desc);
            }
        } else {
            log.info("  分时数据不可用（非交易时段或网络问题）。");
        }

        log.info("{}", sep);
        log.info("  [领涨/跟风关系]");
        if (!report.getLeadLagRelations().isEmpty()) {
            for (var e : report.getLeadLagRelations().entrySet()) {
                LeadLagResult r = e.getValue();
                if (Math.abs(r.getConfidence()) > 0.3) {
                    log.info("  {} 领先 {} 约 {} 分钟 (置信度: {:.2f})", r.getLeaderCode(), r.getFollowerCode(), r.getLeadMinutes(), Math.abs(r.getConfidence()));
                } else {
                    log.info("  {} vs {}: 无明显领先滞后关系 (置信度: {:.2f})", primary.getCode(), e.getKey(), Math.abs(r.getConfidence()));
                }
            }
        } else {
            log.info("  分时数据不可用，无法检测领涨跟风关系。");
        }

        log.info("{}", sep);
        log.info("  [综合结论]");
        log.info("  {}", report.getSummary());
        log.info("{}", sep);
    }
}
