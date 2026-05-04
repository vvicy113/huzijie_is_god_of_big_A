package com.stock.selector;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票选择/过滤接口——责任链模式的基本节点。
 * <p>
 * 每个选择器接收候选股票代码列表和交易日，返回过滤后的子集。
 * 各选择器通过 {@link StockSelectorChain} 串联，前一个的输出作为后一个的输入。
 * 选股器在 analysis 和 backtest 中都会被使用。
 * <p>
 * 实现类需自行从数据库查询所需数据，推荐使用 SQL 批量查询而非逐只遍历。
 */
public interface StockSelector {

    /**
     * 返回唯一标识码，与 {@link com.stock.constants.SelectorConstants} 中定义的常量一一对应。
     * 用于注册表查找和程序识别。
     */
    int getCode();

    /**
     * 对候选列表执行选择/过滤。
     *
     * @param candidates 候选股票代码列表，可能为空（首次调用时由起始选择器自行填充）
     * @param date       目标交易日，用于查询该日的K线数据
     * @return 过滤后的股票代码列表，永不为 null
     */
    List<String> select(List<String> candidates, LocalDate date);
}
