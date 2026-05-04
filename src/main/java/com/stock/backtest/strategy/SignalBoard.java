package com.stock.backtest.strategy;

import com.stock.model.TradeSignal;
import com.stock.model.TradeAction;

import java.util.*;

/**
 * 信号板——Strategy 与 Engine 之间的桥梁。
 * <p>
 * Strategy.evaluate() 往这里添加信号，Engine 从这里按分数降序读取执行交易。
 * 内部 TreeSet 维护排序，HashMap 支持按 stockCode 快速查找和删除。
 */
public class SignalBoard {

    private final TreeSet<TradeSignal> signals = new TreeSet<>();
    private final Map<String, TradeSignal> index = new HashMap<>();

    /** 添加或更新一个信号（同一股票重复添加时保留更高分） */
    public void add(TradeSignal signal) {
        TradeSignal existing = index.get(signal.stockCode());
        if (existing != null) {
            if (signal.score() <= existing.score()) return;
            signals.remove(existing);
        }
        signals.add(signal);
        index.put(signal.stockCode(), signal);
    }

    /** 按股票代码移除 */
    public void remove(String stockCode) {
        TradeSignal s = index.remove(stockCode);
        if (s != null) signals.remove(s);
    }

    /** 最高分信号 */
    public TradeSignal first() {
        return signals.isEmpty() ? null : signals.first();
    }

    /** 前 N 名（按分数降序） */
    public List<TradeSignal> topN(int n) {
        List<TradeSignal> result = new ArrayList<>();
        for (TradeSignal s : signals) {
            if (result.size() >= n) break;
            result.add(s);
        }
        return result;
    }

    /** 全部信号（按分数降序） */
    public List<TradeSignal> all() {
        return new ArrayList<>(signals);
    }

    /** 获取某只股票的信号 */
    public TradeSignal get(String stockCode) {
        return index.get(stockCode);
    }

    public int size() {
        return signals.size();
    }

    public boolean isEmpty() {
        return signals.isEmpty();
    }

    /** 清空，用于新的交易日 */
    public void clear() {
        signals.clear();
        index.clear();
    }

    /** 只保留 action=BUY 且 score > minScore 的信号 */
    public SignalBoard filterBuy(double minScore) {
        SignalBoard filtered = new SignalBoard();
        for (TradeSignal s : signals) {
            if (s.action() == TradeAction.BUY && s.score() > minScore) {
                filtered.add(s);
            }
        }
        return filtered;
    }
}
