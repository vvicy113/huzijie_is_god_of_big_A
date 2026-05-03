# data — 运行时数据

## 文件

| 文件 | 功能 |
|------|------|
| `stocks.db` | SQLite 数据库 — 存储全部股票的日K线数据（表 `daily_kline`），由 `CsvImporter` 生成，通过 `KLineRepository` 查询 |
