# fetcher — 联网数据抓取

通过东方财富公开 API 获取股票实时数据、行业、概念板块和分时数据。

## 文件

| 文件 | 功能 |
|------|------|
| `StockDataFetcher.java` | 数据抓取接口 — 定义 `fetchBasicInfo / fetchIndustry / fetchConcepts / fetchIntradayData` 四个方法 |
| `EastMoneyFetcher.java` | 东方财富实现 — 通过 `push2.eastmoney.com` API 抓取数据，设置 User-Agent + Referer 反爬，请求间隔 600ms。secid 规则：上海 1.{code}，深圳 0.{code} |
| `SinaFinanceFetcher.java` | 新浪财经备用实现 — 占位类，返回空数据，供东方财富 API 失效时切换 |
