package com.stock.analysis.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.model.ConceptInfo;
import com.stock.model.IndustryInfo;
import com.stock.model.IntradayTick;
import com.stock.model.StockInfo;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EastMoneyFetcher implements StockDataFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 15000;

    private final ObjectMapper mapper = new ObjectMapper();

    private String marketCode(String stockCode) {
        return stockCode.startsWith("6") ? "1" : "0";
    }

    @Override
    public StockInfo fetchBasicInfo(String stockCode) {
        try {
            String secId = marketCode(stockCode) + "." + stockCode;
            String url = "http://push2.eastmoney.com/api/qt/stock/get?secid=" + secId
                    + "&fields=f57,f58,f43,f169,f170,f171";
            JsonNode data = fetchJson(url).get("data");
            if (data == null || data.isNull()) {
                return StockInfo.builder().code(stockCode).name("未知").market("未知").build();
            }
            String name = data.has("f58") ? data.get("f58").asText() : "未知";
            double price = data.has("f43") ? data.get("f43").asDouble() / 100.0 : 0;
            double changePct = data.has("f170") ? data.get("f170").asDouble() / 100.0 : 0;
            String market = stockCode.startsWith("6") ? "SH" : "SZ";

            return StockInfo.builder()
                    .code(stockCode)
                    .name(name)
                    .market(market)
                    .fullCode(market + stockCode)
                    .currentPrice(price)
                    .changePercent(changePct)
                    .build();
        } catch (Exception e) {
            return StockInfo.builder().code(stockCode).name("获取失败").market("未知").build();
        }
    }

    @Override
    public IndustryInfo fetchIndustry(String stockCode) {
        try {
            String secId = marketCode(stockCode) + "." + stockCode;
            String url = "http://push2.eastmoney.com/api/qt/stock/get?secid=" + secId
                    + "&fields=f162,f163,f164";
            JsonNode data = fetchJson(url).get("data");
            if (data == null || data.isNull() || !data.has("f162")) {
                return IndustryInfo.builder().industryCode("").industryName("未知").build();
            }
            String industryCode = data.get("f162").asText();
            String industryName = "未知";

            if (!industryCode.isEmpty()) {
                String indUrl = "http://push2.eastmoney.com/api/qt/stock/get?secid=90."
                        + industryCode + "&fields=f12,f14";
                JsonNode indData = fetchJson(indUrl).get("data");
                if (indData != null && !indData.isNull() && indData.has("f14")) {
                    industryName = indData.get("f14").asText();
                }
            }

            return IndustryInfo.builder()
                    .industryCode(industryCode)
                    .industryName(industryName)
                    .build();
        } catch (Exception e) {
            return IndustryInfo.builder().industryCode("").industryName("获取失败").build();
        }
    }

    @Override
    public List<ConceptInfo> fetchConcepts(String stockCode) {
        List<ConceptInfo> concepts = new ArrayList<>();
        try {
            String secId = marketCode(stockCode) + "." + stockCode;
            String url = "http://push2.eastmoney.com/api/qt/slist/get?fltt=2&invt=2"
                    + "&fields=f12,f14&secids=" + secId;
            JsonNode data = fetchJson(url).get("data");
            if (data != null && data.has("diff")) {
                for (JsonNode node : data.get("diff")) {
                    String code = node.get("f12").asText();
                    String name = node.get("f14").asText();
                    if (code.startsWith("BK") && code.length() >= 6) {
                        concepts.add(ConceptInfo.builder()
                                .conceptCode(code)
                                .conceptName(name)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            // return empty list on failure
        }
        return concepts;
    }

    @Override
    public List<IntradayTick> fetchIntradayData(String stockCode) {
        List<IntradayTick> ticks = new ArrayList<>();
        try {
            String secId = marketCode(stockCode) + "." + stockCode;
            String url = "http://push2.eastmoney.com/api/qt/stock/trends2/get?secid=" + secId
                    + "&fields1=f1,f2,f3&fields2=f51,f52,f53,f54,f55,f56,f57"
                    + "&ut=fa5fd1943c7b386f172d6893dbfd32bb&ndays=1";
            JsonNode data = fetchJson(url).get("data");
            if (data != null && data.has("trends")) {
                String[] lines = data.get("trends").asText().split(";");
                for (String line : lines) {
                    if (line.isEmpty()) continue;
                    String[] fields = line.split(",");
                    ticks.add(IntradayTick.builder()
                            .time(fields.length > 0 ? fields[0] : "")
                            .price(fields.length > 1 ? Double.parseDouble(fields[1]) : 0)
                            .avgPrice(fields.length > 2 ? Double.parseDouble(fields[2]) : 0)
                            .volume(fields.length > 3 ? Double.parseDouble(fields[3]) : 0)
                            .turnover(fields.length > 4 ? Double.parseDouble(fields[4]) : 0)
                            .build());
                }
            }
        } catch (Exception e) {
            // return empty list on failure
        }
        return ticks;
    }

    private JsonNode fetchJson(String url) throws IOException {
        String body = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Referer", "https://quote.eastmoney.com/")
                .ignoreContentType(true)
                .timeout(TIMEOUT)
                .execute()
                .body();
        return mapper.readTree(body);
    }
}
