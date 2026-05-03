# engine — 回测引擎核心

逐日遍历K线数据，根据策略信号模拟买卖，记录交易和权益曲线。

## 文件

| 文件 | 功能 |
|------|------|
| `BacktestEngine.java` | 回测引擎 — `run(strategy, klineData, capital, stockCode)` → `BacktestResult`。逐日驱动策略 `evaluate()`，执行 BUY/SELL 信号，计算佣金（万2.5/最低5元）和印花税（千1/仅卖出），记录权益曲线，回测结束时清仓并计算绩效指标 |
