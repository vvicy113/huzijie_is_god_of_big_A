package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 策略上下文——将 evaluate 需要的所有数据封装在一起，并提供快捷方法。
 * <p>
 * 由 Engine 在每日开盘前创建并传入 Strategy.evaluate()。
 */
public class StrategyContext {

    private final List<String> candidates;
    private final Map<String, List<KLine>> klineMap;
    private final Map<String, Position> positions;
    private final double availableCash;
    private final LocalDate date;
    private final SignalBoard signalBoard;

    public StrategyContext(
            List<String> candidates,
            Map<String, List<KLine>> klineMap,
            Map<String, Position> positions,
            double availableCash,
            LocalDate date,
            SignalBoard signalBoard) {
        this.candidates = Collections.unmodifiableList(Objects.requireNonNull(candidates));
        this.klineMap = Collections.unmodifiableMap(Objects.requireNonNull(klineMap));
        this.positions = Objects.requireNonNull(positions);
        this.availableCash = availableCash;
        this.date = Objects.requireNonNull(date);
        this.signalBoard = Objects.requireNonNull(signalBoard);
    }

    // ======================== 原始数据 ========================

    /** 当日候选股票代码列表（不可变） */
    public List<String> candidates() { return candidates; }

    /** stockCode → 历史K线（不可变） */
    public Map<String, List<KLine>> klineMap() { return klineMap; }

    /** stockCode → 当前持仓 */
    public Map<String, Position> positions() { return positions; }

    /** 当前可用资金 */
    public double availableCash() { return availableCash; }

    /** 当前交易日 */
    public LocalDate date() { return date; }

    /** 信号板 */
    public SignalBoard signalBoard() { return signalBoard; }

    // ======================== 快捷方法 ========================

    /** 获取某只股票的今日K线（最后一条） */
    public KLine todayKLine(String stockCode) {
        List<KLine> klines = klineMap.get(stockCode);
        if (klines == null || klines.isEmpty()) return null;
        return klines.get(klines.size() - 1);
    }

    /** 获取某只股票的历史K线列表 */
    public List<KLine> history(String stockCode) {
        List<KLine> klines = klineMap.get(stockCode);
        return klines != null ? klines : Collections.emptyList();
    }

    /** 获取某只股票的持仓状态 */
    public Position position(String stockCode) {
        Position p = positions.get(stockCode);
        return p != null ? p : Position.empty();
    }

    /** 是否持有某只股票 */
    public boolean isHolding(String stockCode) {
        Position p = positions.get(stockCode);
        return p != null && p.isHolding();
    }

    /** 往信号板添加一条信号 */
    public void signal(String stockCode, TradeAction action, double score) {
        signalBoard.add(new com.stock.model.TradeSignal(stockCode, action, score));
    }
}
