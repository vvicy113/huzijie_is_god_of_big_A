# selector — 股票选择器（责任链）

通过责任链模式组合多个选择器，对股票池进行逐级过滤。供 analysis 和 backtest 使用。

## 文件

| 文件 | 功能 |
|------|------|
| `StockSelector.java` | 选择器接口 — `getCode()` 返回 int 标识码 / `select(candidates, date)` 过滤 |
| `StockSelectorChain.java` | 责任链 — 按顺序串联多个 StockSelector，自身也实现 StockSelector，可嵌套 |
| `AllMainBoardSelector.java` | 全量主板 — 从 daily_kline 查询全部 sh/sz 主板股票，责任链起始节点 |
| `VolumeThresholdSelector.java` | 成交量放量 — 当日成交量 ≥ N日均量 × 倍数 |
| `PriceChangeSelector.java` | 涨跌幅区间 — 涨跌幅在 [min%, max%] 之间 |
| `MaAlignmentSelector.java` | 均线多头 — 短期均线 > 长期均线 |
| `NewHighSelector.java` | N日新高 — 收盘价创 N 日最高 |
| `ConsecutiveRiseSelector.java` | 连阳 — 连续 N 日收盘 > 前日收盘 |
| `AmplitudeSelector.java` | 日振幅 — 振幅在 [min%, max%] 之间 |
| `MaDistanceSelector.java` | 均线乖离率 — 收盘距 N 日均线 ≤ maxPct% |
| `GapUpSelector.java` | 跳空高开 — 开盘价 > 前日收盘 × (1 + minPct%) |
| `AmountThresholdSelector.java` | 成交额阈值 — 当日成交额 ≥ minAmount |
| `ShrinkVolumeSelector.java` | 缩量 — 当日成交量 ≤ N日均量 × 倍数 |

## 关联文件

- `com.stock.constants.SelectorConstants` — 常量码定义与描述映射
- `com.stock.registry.StockSelectorRegistry` — 注册表接口
- `com.stock.registry.DefaultSelectorRegistry` — 默认注册表实现
