package com.stock.config;

import java.util.Arrays;

/**
 * 单组批量回测配置——笛卡尔积的一个组合。
 */
public class BatchConfig {

    private final int[] selectorCodes;
    private final int[] scorerCodes;
    private final double[] scorerWeights;
    private final double buyThreshold;
    private final int maxPositions;
    private final double cashRatio;
    private final double stopLossPct;
    private final double takeProfitPct;
    private final int maxHoldingDays;
    private final boolean replaceLowerScored;
    private final boolean allowAddPosition;

    public BatchConfig(int[] selectorCodes, int[] scorerCodes, double[] scorerWeights,
                       double buyThreshold, int maxPositions, double cashRatio,
                       double stopLossPct, double takeProfitPct, int maxHoldingDays,
                       boolean replaceLowerScored, boolean allowAddPosition) {
        this.selectorCodes = selectorCodes;
        this.scorerCodes = scorerCodes;
        this.scorerWeights = scorerWeights;
        this.buyThreshold = buyThreshold;
        this.maxPositions = maxPositions;
        this.cashRatio = cashRatio;
        this.stopLossPct = stopLossPct;
        this.takeProfitPct = takeProfitPct;
        this.maxHoldingDays = maxHoldingDays;
        this.replaceLowerScored = replaceLowerScored;
        this.allowAddPosition = allowAddPosition;
    }

    // Getters
    public int[] selectorCodes() { return selectorCodes; }
    public int[] scorerCodes() { return scorerCodes; }
    public double[] scorerWeights() { return scorerWeights; }
    public double buyThreshold() { return buyThreshold; }
    public int maxPositions() { return maxPositions; }
    public double cashRatio() { return cashRatio; }
    public double stopLossPct() { return stopLossPct; }
    public double takeProfitPct() { return takeProfitPct; }
    public int maxHoldingDays() { return maxHoldingDays; }
    public boolean replaceLowerScored() { return replaceLowerScored; }
    public boolean allowAddPosition() { return allowAddPosition; }

    /** 生成策略名称（用于日志和CSV标识） */
    public String strategyName() {
        return "S" + join(selectorCodes) + "_C" + join(scorerCodes)
                + "_th" + (int) buyThreshold + "_mp" + maxPositions
                + "_cr" + (int)(cashRatio*100) + "_sl" + (int)stopLossPct
                + "_tp" + (int)takeProfitPct + "_hd" + maxHoldingDays
                + (replaceLowerScored ? "_R" : "") + (allowAddPosition ? "_A" : "");
    }

    private static String join(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int v : arr) sb.append(v);
        return sb.toString();
    }

    /** 生成 BacktestConfig */
    public BacktestConfig toBacktestConfig(double initialCapital) {
        return BacktestConfig.builder()
                .initialCapital(initialCapital).maxPositions(maxPositions).cashRatio(cashRatio)
                .stopLossPct(stopLossPct).takeProfitPct(takeProfitPct).maxHoldingDays(maxHoldingDays)
                .replaceLowerScored(replaceLowerScored).allowAddPosition(allowAddPosition)
                .build();
    }
}
