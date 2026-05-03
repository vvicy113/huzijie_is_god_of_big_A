# llm — 大模型综合分析

将量化分析的结构化数据发送给 Claude API，生成自然语言的投资分析解读。

## 文件

| 文件 | 功能 |
|------|------|
| `LlmConfig.java` | 配置加载 — 从 `config.properties` 读取 API Key 和模型名，优先使用环境变量 `ANTHROPIC_API_KEY` |
| `LlmReportService.java` | LLM 分析服务 — 将 `StockRelationReport` 构造为结构化 Prompt，调用 Claude Messages API（Haiku 模型），返回 200-300 字中文综合分析。使用 `java.net.http.HttpClient`，不依赖第三方 SDK |
