# stock-trading-system

股票回测与分析系统。Java 17 + Maven，控制台交互式应用。

## 目录

```
stock-trading-system/
├── pom.xml                     ← Maven 项目配置
├── data/                       ← 运行时数据文件（SQLite 数据库）
├── src/main/java/com/stock/    ← 源代码根包
│   ├── App.java                ← 程序入口
│   ├── cli/                    ← 命令行交互界面
│   ├── model/                  ← 数据模型（POJO）
│   ├── backtest/               ← 回测模块
│   │   ├── engine/             ← 回测引擎核心
│   │   ├── strategy/           ← 交易策略
│   │   ├── metrics/            ← 绩效指标计算
│   │   ├── report/             ← 报告生成与格式化
│   │   └── loader/             ← CSV 数据加载
│   ├── analysis/               ← 股票关联分析模块
│   │   ├── fetcher/            ← 联网数据抓取（东方财富）
│   │   ├── analyzer/           ← 相关性/领涨跟风分析
│   │   └── llm/                ← 大模型综合分析
│   └── db/                     ← SQLite 数据库层
└── src/test/                   ← 单元测试
    └── java/com/stock/
        ├── backtest/           ← 回测模块测试
        └── analysis/           ← 分析模块测试（待补充）
```
