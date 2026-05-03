# analyzer — 关联分析计算

基于分时数据计算股票间的价格相关性和领涨跟风关系。

## 文件

| 文件 | 功能 |
|------|------|
| `RelationAnalyzer.java` | 关联分析协调器 — 调用 `StockDataFetcher` 获取数据 → 行业匹配 → 概念重叠检测 → 调用 `CorrelationCalculator` 和 `LeadLagDetector` → 生成 `StockRelationReport` |
| `CorrelationCalculator.java` | 价格相关性计算 — 基于两只股票分时价格序列计算 Pearson 相关系数（使用 `Commons Math3`） |
| `LeadLagDetector.java` | 领涨跟风检测 — 通过互相关分析不同时间偏移下的收益率相关系数，找到最大相关性的偏移量，判断谁领先谁跟风 |
