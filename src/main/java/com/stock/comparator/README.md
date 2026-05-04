# comparator — 股票打分器

对候选池批量打分，Strategy 根据分数决定买卖优先级。

## 文件

| 文件 | 功能 |
|------|------|
| `StockComparator.java` | 打分器接口 — `getCode()` / `score(codes, date)` → Map<code, score> |
| `MomentumScorer.java` | 动量打分 — N日涨幅越高分越高 |
| `VolumeRatioScorer.java` | 量比打分 — 当日量/N日均量，放量高分 |
| `MaDistanceScorer.java` | 均线乖离打分 — 收盘越贴近均线分越高 |
| `AmplitudeScorer.java` | 振幅打分 — 日振幅越大分越高 |
| `CompositeScorer.java` | 综合打分 — 多个打分器加权求和 |

## 关联文件

- `com.stock.constants.ComparatorConstants` — 常量码定义与描述映射
