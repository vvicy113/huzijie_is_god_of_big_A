# loader — 数据加载

从 CSV 文件或 SQLite 数据库加载日K线数据。

## 文件

| 文件 | 功能 |
|------|------|
| `CsvDataLoader.java` | 日K线数据加载器 — `load(filePath)` 从 CSV 文件读取；`loadFromDesktopStocks(stockCode)` 优先从 SQLite 查询，无数据时回退到 CSV。UTF-8 编码 |
| `MinuteCsvLoader.java` | 分时数据加载器 — `load(filePath)` 从 GBK 编码 CSV 读取；`loadFromCsvDir(stockCode, year)` 从 `csv/{year}/` 目录加载 |
