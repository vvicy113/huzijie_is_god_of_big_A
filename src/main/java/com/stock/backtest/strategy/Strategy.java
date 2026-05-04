package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;
import com.stock.selector.StockSelector;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
     * @param candidates     选股器过滤后的候选股票代码列表
     * @param klineMap       每只候选股票的历史日K线（按日期升序）
     * @param positions      当前持仓，key=stockCode
     * @param availableCash  当前可用资金
     * @param date           当前交易日
     * @param signalBoard    信号板，策略往这里添加 {@link com.stock.model.TradeSignal}
     */
    void evaluate(
            List<String> candidates,
            Map<String, List<KLine>> klineMap,
            Map<String, Position> positions,
            double availableCash,
            LocalDate date,
            SignalBoard signalBoard
    );

    /** 策略重置回调。每次回测开始前调用 */
    default void onReset() {}
}
