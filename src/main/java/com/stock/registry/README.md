# registry — 注册表

管理组件注册和查找。

## 文件

| 文件 | 功能 |
|------|------|
| `StockSelectorRegistry.java` | 选择器注册表接口 — `register(int)` / `register(instance)` / `build()` 构建责任链 |
| `DefaultSelectorRegistry.java` | 默认实现 — 内部工厂 `createFromCode(int)` 可被子类覆盖以扩展自定义选择器 |
