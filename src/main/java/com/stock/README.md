# com.stock — 根包

项目根包，包含程序入口和子模块组织。

## 文件

| 文件 | 功能 |
|------|------|
| `App.java` | 程序入口，启动 `ConsoleUI` 主菜单 |

## 子包

| 包 | 功能 |
|------|------|
| `cli` | 命令行交互界面 |
| `model` | 数据模型（POJO / Enum） |
| `backtest` | 历史回测引擎 |
| `analysis` | 股票关联分析（联网数据 + LLM） |
| `db` | SQLite 数据库读写 |
