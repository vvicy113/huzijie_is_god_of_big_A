# 股票回测与分析系统

Java 17 + Maven。通过 `run.properties` 配置驱动，自动执行。

## 代码规范

### 目录文档

**每个源码目录下必须有 `README.md` 文件**，包含：
- 目录功能的简要说明
- 该目录下每个 Java 文件的表格（文件名 → 功能描述）
- 新增 Java 文件时，必须同步更新所在目录的 README.md
- 新增子包/目录时，必须创建对应的 README.md 并更新父目录 README.md 的子包列表

### 接口规范

**接口必须包含完整的 Javadoc 注释**：
- 类级别：说明接口的职责和设计意图
- 方法级别：说明方法功能、各参数含义、返回值含义

**有多个子类的接口必须提供 `getDesc()` 方法**，返回人类可读的描述文本：
```java
public interface StockSelector {
    int getCode();
    String getDesc();   // 描述文本，如 "成交量筛选(20日均量 × 1.5倍)"
    List<String> select(List<String> candidates, LocalDate date);
}
```

### 日志规范

**全项目统一使用 SLF4J Logger**，禁止 `System.out` / `System.err`：
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("message");   // 业务信息
log.warn("message");   // 警告
log.error("message", e); // 错误+异常栈
```

日志配置位置说明见 `src/main/resources/logback.xml` 和 `com.stock.log.LogSetup`。

### 策略接口

实现 `Strategy` 接口的四个方法，在 `StrategyRegistry` 的 static 块中注册即可：
```java
public class MyStrategy implements Strategy {
    public String getName() { return "我的策略"; }
    public StockSelector getStockSelector() { return ...; }
    public StockComparator getComparator() { return ...; }
    public void evaluate(StrategyContext ctx) { ... }
    // 无状态策略不需要重写 onReset()
}
```

有状态策略（计数器/动态阈值/缓存）必须重写 `onReset()` 重置内部状态。

## 功能

### 1. 回测引擎
- 从 SQLite 加载日K线 → 选股器筛选 → 打分器排序 → 策略决策 → 执行器交易 → 报告
- 数据源：`csv/日k/`（UTF-8，列名：股票代码,日期,开盘价,最高价,最低价,收盘价,昨收价,涨跌额,涨跌幅,成交量,成交额）
- 导入过滤：仅导入 sh/sz + 主板 + 非 ST 的 A 股，同时写入 `stock_info` 元信息表
- 交易成本：佣金万2.5（最低5元），印花税千1（仅卖出）
- 指标：总收益率、年化收益、最大回撤、夏普比率、胜率、盈亏比
- 4个内置策略：动量突破(标准/激进/稳健)、底部反转

### 2. 股票关联分析
- 联网抓取东方财富 API → 行业匹配/概念重叠/价格相关性/领涨跟风 → LLM 综合解读
- 请求间隔600ms，设置 User-Agent + Referer 反爬

## 项目结构

```
├── pom.xml
├── run.properties                 ← 运行配置（数字枚举，UTF-8）
├── run.properties.example         ← 配置模板
├── csv/                           ← 数据源
│   ├── 日k/                       ← 日K线 CSV（主数据源）
│   ├── 股票列表.csv               ← 股票基础信息（导入过滤用）
│   ├── 周k/ / 月k/               ← 暂未启用
├── data/
│   └── stocks.db                  ← SQLite（daily_kline + stock_info）
├── log/                           ← 日志输出（按时间戳子目录）
│   └── yyyy-MM-dd_HH-mm-ss/
│       ├── README.md              ← 本次运行配置记录
│       └── app.log                ← 完整日志
├── doc/                           ← 架构文档
├── src/main/java/com/stock/
│   ├── App.java                   ← 入口
│   ├── cli/          ConsoleUI, BacktestMenu, AnalysisMenu, DataMenu
│   ├── model/        12个模型类
│   ├── config/       BacktestConfig, RunConfig
│   ├── constants/    SelectorConstants, ComparatorConstants
│   ├── selector/     23个选股器 + StockSelectorChain
│   ├── comparator/    5个打分器 + CompositeScorer
│   ├── registry/     StockSelectorRegistry + DefaultSelectorRegistry
│   ├── backtest/
│   │   ├── strategy/  Strategy + SignalBoard + StrategyContext + 4个策略
│   │   ├── engine/    BacktestEngine
│   │   ├── executor/  TradeExecutor + EqualSplitExecutor
│   │   ├── metrics/   MetricsCalculator
│   │   ├── report/    ReportGenerator, ReportFormatter
│   │   └── loader/    CsvDataLoader
│   ├── analysis/
│   │   ├── fetcher/   StockDataFetcher, EastMoneyFetcher, SinaFinanceFetcher
│   │   ├── analyzer/  RelationAnalyzer, CorrelationCalculator, LeadLagDetector
│   │   └── llm/       LlmConfig, LlmReportService
│   ├── db/            DatabaseManager, KLineRepository, CsvImporter, ProjectPaths
│   └── log/           LogSetup
└── src/test/          单元测试
```

## 运行

```bash
# 1. 配置
cp src/main/resources/run.properties.example src/main/resources/run.properties
# 编辑 run.properties（修改 mode / strategy / 日期 / 风控等）

# 2. 编译运行
mvn compile -o
java -cp "target/classes:$(所有依赖jar路径)" com.stock.App
```

日志输出位置：`log/yyyy-MM-dd_HH-mm-ss/app.log`

## 配置文件 reference

```properties
mode=1                      # 1=回测 2=分析 3=导入 4=统计
backtest.strategy=1         # 1=标准 2=激进 3=稳健 4=反转
backtest.date.from=2024-01-01
backtest.date.to=2024-12-31
backtest.capital=100000
backtest.maxPositions=5
backtest.stopLossPct=-1     # -1=禁用
backtest.takeProfitPct=-1   # -1=禁用
```

## 关键依赖

SQLite JDBC · Jsoup · OpenCSV · Commons Math3 · Jackson · Lombok · SLF4J · Logback · JUnit5

## 东方财富 API

- `push2.eastmoney.com/api/qt/stock/get` → 基本信息+行业
- secid 规则：上海 `1.{code}`，深圳 `0.{code}`

## 注意

- Maven 镜像（bilibili nexus）有 SSL 问题，编译用 `-o` 离线模式
- 日K线需先导入 SQLite 后才能回测（run.properties mode=3）
