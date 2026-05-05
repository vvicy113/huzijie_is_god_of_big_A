package com.stock.backtest.strategy;

import com.stock.comparator.StockComparator;
import com.stock.selector.StockSelector;

/**
 * 短线交易策略接口。
 * <p>
 * 每个策略自带选股器、打分器和评估逻辑：
 * <ol>
 *   <li>{@link #getStockSelector()} — 每日开盘前从全市场筛选候选池</li>
 *   <li>{@link #getComparator()} — 对候选池批量打分排序</li>
 *   <li>{@link #evaluate} — 逐只评估，往 SignalBoard 填入 BUY/SELL/HOLD + 分数</li>
 * </ol>
 * Engine 从 SignalBoard 按分数降序读取信号执行交易。
 */
public interface Strategy {

    /** 返回策略名称 */
    String getName();

    /** 返回该策略的选股器 */
    StockSelector getStockSelector();

    /** 返回该策略的打分器 */
    StockComparator getComparator();

    /**
     * 对当日候选池逐只评估，生成交易信号。
     *
     * @param ctx 策略上下文，包含候选池、K线数据、持仓状态、可用资金、信号板等
     */
    void evaluate(StrategyContext ctx);

    /**
     * 策略重置回调，每次回测开始前由 Engine 调用。
     * <p>
     * 策略如果有<b>内部可变状态</b>（如连续亏损天数、动态阈值、缓存），
     * 必须在此方法中重置为初始值，否则上一次回测的脏数据会污染下一次。
     * <p>
     * <b>何时需要重写：</b>
     * <ul>
     *   <li>策略维护了跨交易日的计数器（连阳天数、空仓天数等）</li>
     *   <li>策略有自适应参数（根据市场状态动态调整的阈值）</li>
     *   <li>策略缓存了计算结果（如已计算过的均线值）</li>
     * </ul>
     * <p>
     * <b>何时不需要重写：</b>
     * <ul>
     *   <li>策略完全无状态，所有决策仅依赖当天 ctx 内的数据（如 MomentumBreakoutStrategy）</li>
     * </ul>
     * <p>
     * <b>实现示例：</b>
     * <pre>{@code
     * class MyStatefulStrategy implements Strategy {
     *     private int emptyDays = 0;      // 连续空仓天数
     *     private double dynamicThreshold = 60;
     *
     *     public void onReset() {
     *         emptyDays = 0;
     *         dynamicThreshold = 60;
     *     }
     * }
     * }</pre>
     */
    default void onReset() {}
}
