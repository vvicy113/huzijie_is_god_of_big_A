# cli — 命令行交互界面

基于 `Scanner` 的控制台菜单驱动 UI。

## 文件

| 文件 | 功能 |
|------|------|
| `ConsoleUI.java` | 主菜单，提供回测 / 关联分析 / 数据管理三个入口 |
| `BacktestMenu.java` | 回测子菜单 — 输入股票代码 → 从数据库加载日K线 → 选策略 → 执行回测 → 输出报告 |
| `AnalysisMenu.java` | 关联分析子菜单 — 输入股票代码 → 联网抓取 → 量化分析 → LLM 综合解读 |
| `DataMenu.java` | 数据管理子菜单 — 导入 csv/日k/ 到数据库（仅 sh/sz 主板）/ 查看统计 |
