package com.stock.cli;

import com.stock.analysis.analyzer.RelationAnalyzer;
import com.stock.analysis.llm.LlmConfig;
import com.stock.analysis.llm.LlmReportService;
import com.stock.model.*;

import java.util.*;

public class AnalysisMenu {

    private final Scanner scanner;
    private final RelationAnalyzer analyzer;
    private final LlmReportService llmService;

    public AnalysisMenu(Scanner scanner) {
        this.scanner = scanner;
        this.analyzer = new RelationAnalyzer();
        this.llmService = new LlmReportService();
    }

    public void show() {
        System.out.println();
        System.out.println("  ══════════════ 股票关联分析 ══════════════");

        System.out.println("\n  请输入主分析股票代码（如 600519）:");
        System.out.print("  > ");
        String primaryCode = scanner.nextLine().trim();
        if (primaryCode.isEmpty()) {
            System.out.println("  股票代码不能为空。");
            return;
        }

        System.out.println("\n  请输入对比股票代码，多个用逗号分隔（如 000858,002304,600809）:");
        System.out.print("  > ");
        String compareInput = scanner.nextLine().trim();
        if (compareInput.isEmpty()) {
            System.out.println("  至少需要一只对比股票。");
            return;
        }

        List<String> compareCodes = Arrays.stream(compareInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        System.out.println("\n  正在联网抓取数据（可能需要几秒钟）...");
        StockRelationReport report = analyzer.analyze(primaryCode, compareCodes);
        printReport(report);

        // LLM 综合分析
        if (LlmConfig.isConfigured()) {
            System.out.println("\n  正在生成AI综合分析...");
            String llmAnalysis = llmService.analyze(report);
            if (llmAnalysis != null && !llmAnalysis.isBlank()) {
                System.out.println("  ───────────────────────────────────────────────────");
                System.out.println("  [AI 综合分析]");
                System.out.println("  " + llmAnalysis.trim());
                System.out.println("  ───────────────────────────────────────────────────");
            } else {
                System.out.println("  [提示] AI分析暂不可用，已展示量化指标报告。");
            }
        } else {
            System.out.println("\n  [提示] 未配置大模型API Key，跳过AI综合分析。");
            System.out.println("  如需启用，请在 src/main/resources/config.properties 中配置 anthropic.api.key。");
        }

        System.out.print("\n  按回车键返回主菜单...");
        scanner.nextLine();
    }

    private void printReport(StockRelationReport report) {
        int w = 56;
        String sep = "─".repeat(w);

        System.out.println();
        System.out.println(sep);
        System.out.printf("  股票关联分析报告%n");
        System.out.println(sep);

        // 基本信息
        StockInfo primary = report.getPrimaryStock();
        System.out.printf("  主股票: %s(%s)  %.2f元%n",
                primary.getName(), primary.getCode(), primary.getCurrentPrice());

        System.out.println(sep);
        System.out.println("  [行业板块]");
        System.out.printf("  主股票行业: %s%n", report.getPrimaryIndustry());
        if (!report.getSameIndustryStocks().isEmpty()) {
            System.out.print("  同行业股票: ");
            for (StockInfo s : report.getSameIndustryStocks()) {
                System.out.print(s.getName() + "(" + s.getCode() + ") ");
            }
            System.out.println();
        } else {
            System.out.println("  无同行业股票。");
        }

        // 概念重叠
        System.out.println(sep);
        System.out.println("  [概念/题材重叠]");
        if (!report.getCommonConcepts().isEmpty()) {
            System.out.println("  共同概念: " + String.join(", ", report.getCommonConcepts()));
            if (report.getConceptDetail() != null) {
                for (Map.Entry<String, List<StockInfo>> e : report.getConceptDetail().entrySet()) {
                    List<String> names = e.getValue().stream()
                            .map(s -> s.getName() + "(" + s.getCode() + ")")
                            .toList();
                    System.out.printf("    - \"%s\": %s%n", e.getKey(), String.join(", ", names));
                }
            }
        } else {
            System.out.println("  无共同概念板块。");
        }

        // 价格相关性
        System.out.println(sep);
        System.out.println("  [价格相关性 (分时数据Pearson系数)]");
        if (!report.getPriceCorrelation().isEmpty()) {
            for (Map.Entry<String, Double> e : report.getPriceCorrelation().entrySet()) {
                String desc = Math.abs(e.getValue()) > 0.7 ? "高度相关" :
                        Math.abs(e.getValue()) > 0.4 ? "中度相关" : "弱相关";
                System.out.printf("  %s vs %s: %+.3f (%s)%n",
                        primary.getCode(), e.getKey(), e.getValue(), desc);
            }
        } else {
            System.out.println("  分时数据不可用（非交易时段或网络问题）。");
        }

        // 领涨跟风
        System.out.println(sep);
        System.out.println("  [领涨/跟风关系]");
        if (!report.getLeadLagRelations().isEmpty()) {
            for (Map.Entry<String, LeadLagResult> e : report.getLeadLagRelations().entrySet()) {
                LeadLagResult r = e.getValue();
                if (Math.abs(r.getConfidence()) > 0.3) {
                    System.out.printf("  %s 领先 %s 约 %d 分钟 (置信度: %.2f)%n",
                            r.getLeaderCode(), r.getFollowerCode(),
                            r.getLeadMinutes(), Math.abs(r.getConfidence()));
                } else {
                    System.out.printf("  %s vs %s: 无明显领先滞后关系 (置信度: %.2f)%n",
                            primary.getCode(), e.getKey(), Math.abs(r.getConfidence()));
                }
            }
        } else {
            System.out.println("  分时数据不可用，无法检测领涨跟风关系。");
        }

        // 综合结论
        System.out.println(sep);
        System.out.println("  [综合结论]");
        System.out.println("  " + report.getSummary());

        System.out.println(sep);
    }
}
