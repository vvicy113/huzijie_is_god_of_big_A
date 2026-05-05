package com.stock.analysis.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stock.model.LeadLagResult;
import com.stock.model.StockInfo;
import com.stock.model.StockRelationReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 调用 Claude API 对 StockRelationReport 生成自然语言综合分析。
 */
public class LlmReportService {

    private static final Logger log = LoggerFactory.getLogger(LlmReportService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final int TIMEOUT_SECONDS = 60;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public LlmReportService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * 基于关联分析报告生成自然语言分析。
     *
     * @return LLM 生成的分析文本，若未配置或失败则返回 null
     */
    public String analyze(StockRelationReport report) {
        if (!LlmConfig.isConfigured()) {
            return null;
        }

        String prompt = buildPrompt(report);
        String systemPrompt = "你是一位专业的A股市场分析师，擅长解读股票之间的关联关系。"
                + "请基于提供的量化指标数据，用自然流畅的中文解读这些股票之间的关系。"
                + "不要重复原始数据，而是用分析师的口吻提炼关键发现。"
                + "注意：不构成投资建议，仅提供客观分析。";

        try {
            String requestBody = buildRequestBody(systemPrompt, prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key", LlmConfig.getApiKey())
                    .header("anthropic-version", API_VERSION)
                    .header("content-type", "application/json")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode content = root.path("content");
                if (content.isArray() && content.size() > 0) {
                    return content.get(0).path("text").asText();
                }
            } else {
                log.error("API 调用失败: HTTP {}", response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("调用异常", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return null;
    }

    private String buildRequestBody(String systemPrompt, String userMessage) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", LlmConfig.getModel());
        body.put("max_tokens", 1024);

        // system prompt
        body.put("system", systemPrompt);

        // messages
        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        body.set("messages", messages);

        return mapper.writeValueAsString(body);
    }

    private String buildPrompt(StockRelationReport report) {
        StringBuilder sb = new StringBuilder();
        StockInfo primary = report.getPrimaryStock();

        sb.append("请分析以下股票关联数据：\n\n");

        // 主股票信息
        sb.append("## 主股票\n");
        sb.append("- 名称: ").append(primary.getName())
                .append(" (").append(primary.getCode()).append(")\n");
        sb.append("- 最新价: ").append(String.format("%.2f", primary.getCurrentPrice())).append("元\n");
        sb.append("- 行业: ").append(report.getPrimaryIndustry()).append("\n\n");

        // 对比股票
        List<StockInfo> compared = report.getComparedStocks();
        if (compared != null && !compared.isEmpty()) {
            sb.append("## 对比股票\n");
            for (StockInfo s : compared) {
                sb.append("- ").append(s.getName()).append(" (").append(s.getCode()).append(")\n");
            }
            sb.append("\n");
        }

        // 同行业
        List<StockInfo> sameIndustry = report.getSameIndustryStocks();
        if (sameIndustry != null && !sameIndustry.isEmpty()) {
            sb.append("## 同行业股票\n");
            for (StockInfo s : sameIndustry) {
                sb.append("- ").append(s.getName()).append(" (").append(s.getCode()).append(")\n");
            }
            sb.append("\n");
        } else {
            sb.append("## 同行业股票\n无\n\n");
        }

        // 概念重叠
        List<String> commonConcepts = report.getCommonConcepts();
        if (commonConcepts != null && !commonConcepts.isEmpty()) {
            sb.append("## 共同概念板块\n");
            sb.append(String.join("、", commonConcepts)).append("\n\n");
        } else {
            sb.append("## 共同概念板块\n无\n\n");
        }

        // 价格相关性
        Map<String, Double> correlations = report.getPriceCorrelation();
        if (correlations != null && !correlations.isEmpty()) {
            sb.append("## 价格相关性（Pearson系数）\n");
            for (Map.Entry<String, Double> e : correlations.entrySet()) {
                String level = Math.abs(e.getValue()) > 0.7 ? "高度相关" :
                        Math.abs(e.getValue()) > 0.4 ? "中度相关" : "弱相关";
                sb.append("- ").append(primary.getCode()).append(" vs ").append(e.getKey())
                        .append(": ").append(String.format("%+.3f", e.getValue()))
                        .append("（").append(level).append("）\n");
            }
            sb.append("\n");
        }

        // 领涨跟风
        Map<String, LeadLagResult> leadLag = report.getLeadLagRelations();
        if (leadLag != null && !leadLag.isEmpty()) {
            sb.append("## 领涨/跟风关系\n");
            for (Map.Entry<String, LeadLagResult> e : leadLag.entrySet()) {
                LeadLagResult r = e.getValue();
                if (Math.abs(r.getConfidence()) > 0.3) {
                    sb.append("- ").append(r.getLeaderCode()).append(" 领先 ")
                            .append(r.getFollowerCode()).append(" 约 ")
                            .append(r.getLeadMinutes()).append(" 分钟")
                            .append("（置信度: ").append(String.format("%.2f", Math.abs(r.getConfidence()))).append("）\n");
                }
            }
            sb.append("\n");
        }

        sb.append("请基于以上数据,撰写一段200-300字的综合分析,涵盖：");
        sb.append("1) 这些股票之间的整体关联度如何；");
        sb.append("2) 最重要的关联关系是什么（行业、概念或价格）；");
        sb.append("3) 哪些股票之间存在领先滞后关系。");

        return sb.toString();
    }
}
