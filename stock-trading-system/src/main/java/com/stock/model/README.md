# model — 数据模型

纯数据对象（POJO），使用 Lombok `@Data/@Builder/@AllArgsConstructor`，无业务逻辑。

## 文件

| 文件 | 功能 |
|------|------|
| `KLine.java` | 日K线数据（日期/开/高/低/收/量/额），通过 OpenCSV 绑定中文列名 |
| `TradeRecord.java` | 单笔交易记录（买卖日期/价格/股数/佣金/盈亏/持仓天数） |
| `TradeAction.java` | 交易动作枚举：BUY / SELL / HOLD |
| `Position.java` | 持仓状态（是否持仓/股数/成本/市值） |
| `BacktestResult.java` | 回测结果聚合（策略/指标/交易列表/权益曲线） |
| `PerformanceMetrics.java` | 绩效指标（收益率/回撤/夏普/胜率/盈亏比） |
| `StockInfo.java` | 股票基本信息（代码/名称/市场/最新价/涨跌幅） |
| `IndustryInfo.java` | 行业信息（行业代码/名称/板块） |
| `ConceptInfo.java` | 概念板块信息（概念代码/名称/成分股列表） |
| `IntradayTick.java` | 分时数据 Tick（时间/价格/均价/量/换手率） |
| `LeadLagResult.java` | 领涨跟风检测结果（领先方/跟风方/领先分钟/置信度） |
| `StockRelationReport.java` | 股票关联分析报告（聚合行业/概念/相关性/领涨结果） |
| `MinuteData.java` | 分时数据（日期/时间/开/高/低/收/量/额），通过 OpenCSV 绑定 GBK 中文列名 |
