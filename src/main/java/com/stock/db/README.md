# db — SQLite 数据库层

管理 SQLite 数据库，提供日K线数据导入和查询。

## 文件

| 文件 | 功能 |
|------|------|
| `DatabaseManager.java` | 数据库管理器（单例） — 管理 `data/stocks.db` 连接，自动建表 `daily_kline`（WITHOUT ROWID），默认 WAL 模式。提供 `setBulkImportMode()` / `setNormalMode()` 用于导入加速 |
| `KLineRepository.java` | 日K线数据仓库 — `findByStockCode(code)` 按日期升序查询；`exists(code)` 判断是否已入库；`getAllStockCodes()` / `getStockCount()` / `getTotalRows()` 统计查询 |
| `CsvImporter.java` | CSV 导入器 — `importAll()` 先读取 `股票列表.csv` 筛选 sh/sz 前缀 + 主板股票，再从 `csv/日k/` 批量导入（5000行/批次），`INSERT OR IGNORE` 支持断点续传 |
