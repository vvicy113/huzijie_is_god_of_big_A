package com.stock.model;

/**
 * 交易信号——Strategy 的输出，SignalBoard 的条目。
 * <p>
 * 按 score 降序排序（分数高的在前），分数相同时按 stockCode 字典序。
 */
public record TradeSignal(
        String stockCode,
        TradeAction action,
        double score
) implements Comparable<TradeSignal> {

    @Override
    public int compareTo(TradeSignal o) {
        int cmp = Double.compare(o.score, this.score); // 降序
        if (cmp != 0) return cmp;
        return this.stockCode.compareTo(o.stockCode);
    }
}
