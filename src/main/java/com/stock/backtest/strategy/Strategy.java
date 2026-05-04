package com.stock.backtest.strategy;

import com.stock.selector.StockSelector;

/**
 * 短线交易策略接口。
 * <p>
 * 每个策略自带选股器和评估逻辑：
 * <ol>
 *   <li>{@link #getStockSelector()} — 每日开盘前从全市场筛选候选池</li>
 *   <li>{@link #evaluate} — 对候选池逐只评估，往 SignalBoard 填入 BUY/SELL/HOLD + 分数</li>
 * </ol>
 * Engine 从 SignalBoard 按分数降序读取信号执行交易。
 */
public interface Strategy {

    /** 返回策略名称 */
    String getName();

    /** 返回该策略的选股器 */
    StockSelector getStockSelector();

    /**
     * 对当日候选池逐只评估，生成交易信号。
     *
     * @param ctx 策略上下文，包含候选池、K线数据、持仓状态、可用资金、信号板等
     */
    void evaluate(StrategyContext ctx);

    /** 策略重置回调。每次回测开始前调用 */
    default void onReset() {}
}
