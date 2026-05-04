package com.stock.config;

/**
 * 回测配置——集中管理所有可调参数。
 * <p>
 * 使用 Builder 链式构建，未设置的参数使用默认值。
 */
public class BacktestConfig {

    // === 交易成本 ===
    private final double commissionRate;
    private final double minCommission;
    private final double stampTaxRate;

    // === 仓位管理 ===
    private final int maxPositions;
    private final double cashRatio;
    private final boolean allowRebuy;
    private final boolean allowAddPosition;
    private final boolean replaceLowerScored;

    // === 风控 ===
    private final double stopLossPct;
    private final double takeProfitPct;
    private final int maxHoldingDays;

    // === 资金 & 清仓 ===
    private final double initialCapital;
    private final boolean forceCloseAtEnd;

    // === 评估 ===
    private final double riskFreeRate;

    private BacktestConfig(Builder b) {
        this.commissionRate = b.commissionRate;
        this.minCommission = b.minCommission;
        this.stampTaxRate = b.stampTaxRate;
        this.maxPositions = b.maxPositions;
        this.cashRatio = b.cashRatio;
        this.allowRebuy = b.allowRebuy;
        this.allowAddPosition = b.allowAddPosition;
        this.replaceLowerScored = b.replaceLowerScored;
        this.stopLossPct = b.stopLossPct;
        this.takeProfitPct = b.takeProfitPct;
        this.maxHoldingDays = b.maxHoldingDays;
        this.initialCapital = b.initialCapital;
        this.forceCloseAtEnd = b.forceCloseAtEnd;
        this.riskFreeRate = b.riskFreeRate;
    }

    public static Builder builder() { return new Builder(); }
    public static BacktestConfig defaults() { return new Builder().build(); }

    // === Getters ===
    public double commissionRate() { return commissionRate; }
    public double minCommission() { return minCommission; }
    public double stampTaxRate() { return stampTaxRate; }
    public int maxPositions() { return maxPositions; }
    public double cashRatio() { return cashRatio; }
    public boolean allowRebuy() { return allowRebuy; }
    public boolean allowAddPosition() { return allowAddPosition; }
    public boolean replaceLowerScored() { return replaceLowerScored; }
    public double stopLossPct() { return stopLossPct; }
    public double takeProfitPct() { return takeProfitPct; }
    public int maxHoldingDays() { return maxHoldingDays; }
    public double initialCapital() { return initialCapital; }
    public boolean forceCloseAtEnd() { return forceCloseAtEnd; }
    public double riskFreeRate() { return riskFreeRate; }

    /** 止损是否启用 */
    public boolean stopLossEnabled() { return stopLossPct > 0; }
    /** 止盈是否启用 */
    public boolean takeProfitEnabled() { return takeProfitPct > 0; }
    /** 最大持仓天数是否启用 */
    public boolean maxHoldingDaysEnabled() { return maxHoldingDays > 0; }

    public static class Builder {
        private double commissionRate = 0.00025;
        private double minCommission = 5.0;
        private double stampTaxRate = 0.001;
        private int maxPositions = 5;
        private double cashRatio = 1.0;
        private boolean allowRebuy = false;
        private boolean allowAddPosition = false;
        private boolean replaceLowerScored = false;
        private double stopLossPct = -1;
        private double takeProfitPct = -1;
        private int maxHoldingDays = -1;
        private double initialCapital = 100_000;
        private boolean forceCloseAtEnd = true;
        private double riskFreeRate = 0.02;

        public Builder commissionRate(double v) { commissionRate = v; return this; }
        public Builder minCommission(double v) { minCommission = v; return this; }
        public Builder stampTaxRate(double v) { stampTaxRate = v; return this; }
        public Builder maxPositions(int v) { maxPositions = v; return this; }
        public Builder cashRatio(double v) { cashRatio = v; return this; }
        public Builder allowRebuy(boolean v) { allowRebuy = v; return this; }
        public Builder allowAddPosition(boolean v) { allowAddPosition = v; return this; }
        public Builder replaceLowerScored(boolean v) { replaceLowerScored = v; return this; }
        public Builder stopLossPct(double v) { stopLossPct = v; return this; }
        public Builder takeProfitPct(double v) { takeProfitPct = v; return this; }
        public Builder maxHoldingDays(int v) { maxHoldingDays = v; return this; }
        public Builder initialCapital(double v) { initialCapital = v; return this; }
        public Builder forceCloseAtEnd(boolean v) { forceCloseAtEnd = v; return this; }
        public Builder riskFreeRate(double v) { riskFreeRate = v; return this; }

        public BacktestConfig build() {
            if (commissionRate < 0) throw new IllegalArgumentException("commissionRate < 0");
            if (maxPositions < 1) throw new IllegalArgumentException("maxPositions < 1");
            if (cashRatio <= 0 || cashRatio > 1) throw new IllegalArgumentException("cashRatio not in (0,1]");
            return new BacktestConfig(this);
        }
    }
}
