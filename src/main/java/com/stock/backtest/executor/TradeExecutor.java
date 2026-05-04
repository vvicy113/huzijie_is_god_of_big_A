package com.stock.backtest.executor;

import com.stock.backtest.strategy.SignalBoard;
import com.stock.model.Position;
import com.stock.model.TradeRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 交易执行器接口——将 SignalBoard 中的信号转换为实际买卖。
 * <p>
 * Engine 负责选股→打分→评估→生成信号，Executor 负责执行交易。
 * 不同实现对应不同的资金分配和仓位管理策略。
 */
public interface TradeExecutor {

    /** 返回执行器名称 */
    String getName();

    /**
     * 根据信号板执行买卖。
     *
     * @param board      策略生成的信号（按分数降序）
     * @param positions  当前持仓 Map，会被修改（买入新增、卖出移除）
     * @param cash       当前可用资金，会被修改（买入扣减、卖出增加）
     * @param prices     每只股票的当日收盘价（用于计算买卖金额）
     * @param date       当前交易日
     * @return 当日实际成交记录
     */
    List<TradeRecord> execute(
            SignalBoard board,
            Map<String, Position> positions,
            DoubleRef cash,
            Map<String, Double> prices,
            LocalDate date
    );
}
