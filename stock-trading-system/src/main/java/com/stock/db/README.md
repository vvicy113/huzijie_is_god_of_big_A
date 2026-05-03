# db — SQLite 数据库层

将 CSV 日K线数据导入 SQLite，支持快速按股票代码查询。

## 文件

| 文件 | 功能 |
|------|------|
| `DatabaseManager.java` | 数据库管理器（单例） — 管理 `data/stocks.db` 连接，自动建表 `daily_kline` + `minute_kline`（WITHOUT ROWID），默认 WAL 模式。提供 `setBulkImportMode()` / `setNormalMode()` 用于导入加速 |
| `KLineRepository.java` | 数据仓库 — 日K线：`findByStockCode()` / `exists()` / `getStockCount()` / `getTotalRows()`；分时：`findMinuteByStockCode()` / `existsMinute()` / `getMinuteTotalRows()` |
| `CsvImporter.java` | CSV 导入器 — `importAll()` 导入日K线（UTF-8）；`importMinuteData(year)` 导入分时数据（GBK）。批量 INSERT（5000行/批次），`INSERT OR IGNORE` 支持断点续传 |
