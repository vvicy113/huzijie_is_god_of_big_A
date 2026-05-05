package com.stock.backtest.strategy;

import com.stock.comparator.*;
import com.stock.constants.ComparatorConstants;
import com.stock.constants.SelectorConstants;
import com.stock.model.TradeAction;
import com.stock.registry.DefaultSelectorRegistry;
import com.stock.selector.StockSelector;

import java.util.*;

/**
 * 动量突破策略——参数化短线策略。
 * <p>
 * 选股链、打分权重、买入阈值均可通过构造函数配置，
 * 在 StrategyRegistry 中用不同参数注册即可得到不同策略变体。
 */
public class MomentumBreakoutStrategy implements Strategy {

    private final String name;
    private final StockSelector selector;
    private final StockComparator comparator;
    private final double buyThreshold;

    /**
     * @param name          策略名称
     * @param selectorCodes 选股器 code 列表（按顺序执行）
     * @param scorerCodes   打分器 code 列表
     * @param scorerWeights 对应权重（与 scorerCodes 一一对应，和必须为1.0）
     * @param buyThreshold  买入分数阈值
     */
    public MomentumBreakoutStrategy(
            String name,
            int[] selectorCodes,
            int[] scorerCodes,
            double[] scorerWeights,
            double buyThreshold) {

        this.name = name;
        this.buyThreshold = buyThreshold;

        // 构建选股链
        DefaultSelectorRegistry reg = new DefaultSelectorRegistry(name + "选股");
        for (int code : selectorCodes) reg.register(code);
        this.selector = reg.build();

        // 构建综合打分器
        Map<StockComparator, Double> scorerMap = new LinkedHashMap<>();
        for (int i = 0; i < scorerCodes.length; i++) {
            StockComparator sc = createScorer(scorerCodes[i]);
            scorerMap.put(sc, scorerWeights[i]);
        }
        this.comparator = new CompositeScorer(
                ComparatorConstants.COMPOSITE_MOM_VOL, name + "打分", scorerMap);
    }

    @Override public String getName() { return name; }
    @Override public StockSelector getStockSelector() { return selector; }
    @Override public StockComparator getComparator() { return comparator; }

    @Override
    public void evaluate(StrategyContext ctx) {
        List<String> candidates = ctx.candidates();
        if (candidates.isEmpty()) return;

        Map<String, Double> scores = comparator.score(candidates, ctx.date());

        for (String code : candidates) {
            double score = scores.getOrDefault(code, 0.0);
            if (ctx.isHolding(code)) {
                ctx.signal(code, score > buyThreshold ? TradeAction.HOLD : TradeAction.SELL, score);
            } else if (score > buyThreshold) {
                ctx.signal(code, TradeAction.BUY, score);
            }
        }
    }

    private static StockComparator createScorer(int code) {
        return switch (code) {
            case ComparatorConstants.MOMENTUM_10 -> new MomentumScorer(10, code);
            case ComparatorConstants.MOMENTUM_20 -> new MomentumScorer(20, code);
            case ComparatorConstants.VOL_RATIO_5  -> new VolumeRatioScorer(5, code);
            case ComparatorConstants.VOL_RATIO_20 -> new VolumeRatioScorer(20, code);
            case ComparatorConstants.MA_DIST_5    -> new MaDistanceScorer(5, code);
            case ComparatorConstants.MA_DIST_20   -> new MaDistanceScorer(20, code);
            case ComparatorConstants.AMPLITUDE    -> new AmplitudeScorer(code);
            default -> throw new IllegalArgumentException("不支持的打分器 code: " + code);
        };
    }
}
