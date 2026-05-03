package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.util.List;

/**
 * 交易策略接口。
 * <p>
 * 回测引擎逐日遍历K线数据，每到一个交易日调用 {@link #evaluate} 获取交易信号，
 * 根据返回的 BUY / SELL / HOLD 执行模拟买卖。
 * <p>
 * 新增策略只需实现此接口的三个方法，然后在 {@link StrategyRegistry} 的 static 块中注册即可。
 */
public interface Strategy {

    /**
     * 返回策略名称，用于菜单展示和回测报告。
     * 例如 "均线交叉(5,20)"、"MACD金叉死叉(12,26,9)"。
     */
    String getName();

    /**
     * 评估当前交易日应执行的交易动作。
     *
     * @param currentIndex   当前K线在历史列表中的索引（从0开始）
     * @param history        该股票的全部历史日K线数据（按日期升序），长度 &gt; currentIndex
     * @param currentPosition 当前持仓状态，{@link Position#isHolding()} 判断是否持仓
     * @return BUY（买入信号） / SELL（卖出信号） / HOLD（不操作）
     */
    TradeAction evaluate(int currentIndex, List<KLine> history, Position currentPosition);

    /**
     * 策略重置回调。每次回测开始前调用，用于清理策略内部状态（如技术指标缓存）。
     * 默认空实现。
     */
    default void onReset() {}
}
