# 股票回测与分析系统

Java 17 + Maven，位于 `stock-trading-system/`。

## 代码文档规范

**每个源码目录下必须有 `README.md` 文件**，包含：
- 目录功能的简要说明
- 该目录下每个 Java 文件的表格（文件名 → 功能描述）
- 新增 Java 文件时，必须同步更新所在目录的 README.md
- 新增子包/目录时，必须创建对应的 README.md 并更新父目录 README.md 的子包列表

## 两大功能

### 1. 回测引擎
- 导入历史日K线 CSV → 选择策略 → 模拟交易 → 输出报告
- CSV 格式：`date,open,high,low,close,volume,amount`，按日期升序，文件命名 `SH{code}.csv` 或 `SZ{code}.csv`
- 交易成本：佣金万2.5（最低5元），印花税千1（仅卖出），初始资金默认10万
- 指标：总收益率、年化收益、最大回撤、夏普比率、胜率、盈亏比
- 5个内置策略：均线交叉(5,20)/(10,30)、MACD金叉死叉、突破策略(20日)/(60日)

### 2. 股票关联分析
- 输入股票代码 → 东方财富联网抓取 → 输出四个维度关系报告
- 行业匹配、概念/题材重叠、价格相关性(Pearson)、领涨/跟风(互相关)
- 请求间隔600ms，设置 User-Agent + Referer 反爬

## 项目结构

```
stock-trading-system/
├── pom.xml
├── data/csv/         ← CSV文件放这里
├── src/main/java/com/stock/
│   ├── App.java                    ← 入口
│   ├── cli/          ConsoleUI, BacktestMenu, AnalysisMenu
│   ├── model/        12个模型类 (KLine, TradeRecord, BacktestResult, StockInfo...)
│   ├── backtest/
│   │   ├── strategy/  Strategy接口, 5个策略实现, StrategyRegistry
│   │   ├── engine/    BacktestEngine
│   │   ├── metrics/   MetricsCalculator
│   │   ├── report/    ReportGenerator, ReportFormatter
│   │   └── loader/    CsvDataLoader
│   └── analysis/
│       ├── fetcher/   StockDataFetcher接口, EastMoneyFetcher, SinaFinanceFetcher
│       └── analyzer/  RelationAnalyzer, CorrelationCalculator, LeadLagDetector
└── src/test/          4个单元测试
```

## 依赖

Jsoup(网页抓取) · OpenCSV(CSV解析) · Commons Math3(统计) · Jackson(JSON) · Lombok · Logback · JUnit5

## 运行

```bash
cd stock-trading-system
mvn package -DskipTests
java -jar target/stock-trading-system-1.0.0.jar
```

## 添加新策略

实现 `Strategy` 接口的三个方法，在 `StrategyRegistry` 的 static 块中注册即可。

## 东方财富 API 端点

- `push2.eastmoney.com/api/qt/stock/get` → 基本信息+行业
- `push2.eastmoney.com/api/qt/slist/get` → 板块列表
- `push2.eastmoney.com/api/qt/stock/trends2/get` → 分时数据
- secid 规则：上海 `1.{code}`，深圳 `0.{code}`
- 行情需在交易时段才有数据

## 注意事项

- Maven 镜像（bilibili nexus）可能有 SSL 问题，编译已缓存成功
- 示例 CSV 数据为模拟数据，真实回测需替换为历史数据
- 交易时段外，分时数据 API 返回空
