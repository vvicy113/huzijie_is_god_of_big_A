# selector — 股票选择器（责任链）

通过责任链模式组合多个选择器，对股票池进行逐级过滤。供 analysis 和 backtest 使用。

## 文件

| 文件 | 功能 |
|------|------|
| `StockSelector.java` | 选择器接口 — `getCode()` 返回 int 标识码 / `select(candidates, date)` 过滤 |
| `StockSelectorChain.java` | 责任链 — 按顺序串联多个 StockSelector，自身也实现 StockSelector，可嵌套 |
| `AllMainBoardSelector.java` | 全量主板 — 从 daily_kline 查询全部 sh/sz 主板股票，责任链起始节点 |
| `VolumeThresholdSelector.java` | 成交量阈值 — 当日成交量 ≥ N日均量 × 倍数 |
| `PriceChangeSelector.java` | 涨跌幅区间 — 涨跌幅在 [min%, max%] 之间 |
| `MaAlignmentSelector.java` | 均线多头 — 短期均线 > 长期均线 |

## 关联文件

- `com.stock.constants.SelectorConstants` — 常量码定义与描述映射
- `com.stock.registry.StockSelectorRegistry` — 注册表接口
- `com.stock.registry.DefaultSelectorRegistry` — 默认注册表实现
