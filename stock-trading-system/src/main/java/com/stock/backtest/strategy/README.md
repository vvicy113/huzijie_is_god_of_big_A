# strategy — 交易策略

策略接口定义和内置策略实现。所有策略通过 `StrategyRegistry` 统一注册和查找。

## 文件

| 文件 | 功能 |
|------|------|
| `Strategy.java` | 策略接口 — `getName()` / `evaluate(index, history, position)` / `onReset()` |
| `StrategyRegistry.java` | 策略注册表 — `LinkedHashMap` 存储，static 块注册 5 个内置策略 |
| `MovingAverageCrossStrategy.java` | 均线交叉策略 — 短/长均线金叉买入，死叉卖出。注册：(5,20) 和 (10,30) |
| `GoldenCrossStrategy.java` | MACD 金叉死叉策略 — DIF(12,26) 上穿 DEA(9) 买入，下穿卖出 |
| `BreakoutStrategy.java` | N日突破策略 — 收盘价创N日新高且空仓→买入，创N日新低且持仓→卖出。注册：(20) 和 (60) |
