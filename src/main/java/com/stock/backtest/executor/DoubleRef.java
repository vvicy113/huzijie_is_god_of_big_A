package com.stock.backtest.executor;

/**
 * 可变 double 包装器 —— 用于 TradeExecutor.execute() 中修改可用资金。
 */
public class DoubleRef {
    private double value;

    public DoubleRef(double value) {
        this.value = value;
    }

    public double get() { return value; }
    public void set(double value) { this.value = value; }
    public void add(double delta) { this.value += delta; }
    public void subtract(double delta) { this.value -= delta; }
}
