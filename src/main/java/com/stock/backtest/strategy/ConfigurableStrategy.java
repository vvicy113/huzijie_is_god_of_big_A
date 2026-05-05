package com.stock.backtest.strategy;

import com.stock.comparator.*;
import com.stock.constants.ComparatorConstants;
import com.stock.model.TradeAction;
import com.stock.registry.DefaultSelectorRegistry;
import com.stock.selector.StockSelector;

import java.util.*;

/**
 * 可配置策略——通过 run.properties 指定选股链和打分链，无需写 Java 代码。
 * <p>
 * 配置文件示例：
 * <pre>
 * backtest.selector.codes=1,201,401
 * backtest.scorer.codes=501,601,802
 * backtest.scorer.weights=0.5,0.3,0.2
 * backtest.buyThreshold=60
 * </pre>
 */
public class ConfigurableStrategy implements Strategy {

    private final String name;
    private final StockSelector selector;
    private final StockComparator comparator;
    private final double buyThreshold;

    /**
     * @param name           策略名称（用于日志和报告）
     * @param selectorCodes  选股器 code 列表
     * @param scorerCodes    打分器 code 列表
     * @param scorerWeights  打分权重（与 scorerCodes 一一对应）
     * @param buyThreshold   买入分数阈值
     */
    public ConfigurableStrategy(String name, int[] selectorCodes,
                                int[] scorerCodes, double[] scorerWeights,
                                double buyThreshold) {
        this.name = name;
        this.buyThreshold = buyThreshold;

        // 选股链
        DefaultSelectorRegistry reg = new DefaultSelectorRegistry(name);
        for (int c : selectorCodes) reg.register(c);
        this.selector = reg.build();

        // 综合打分器
        Map<StockComparator, Double> map = new LinkedHashMap<>();
        for (int i = 0; i < scorerCodes.length; i++) {
            map.put(createScorer(scorerCodes[i]), scorerWeights[i]);
        }
        this.comparator = new CompositeScorer(ComparatorConstants.COMPOSITE_MOM_VOL, name, map);
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

    /** 根据 code 创建打分器实例 */
    public static StockComparator createScorer(int code) {
        return switch (code) {
            case ComparatorConstants.MOMENTUM_10       -> new MomentumScorer(10, code);
            case ComparatorConstants.MOMENTUM_20       -> new MomentumScorer(20, code);
            case ComparatorConstants.VOL_RATIO_5       -> new VolumeRatioScorer(5, code);
            case ComparatorConstants.VOL_RATIO_20      -> new VolumeRatioScorer(20, code);
            case ComparatorConstants.MA_DIST_5         -> new MaDistanceScorer(5, code);
            case ComparatorConstants.MA_DIST_20        -> new MaDistanceScorer(20, code);
            case ComparatorConstants.AMPLITUDE         -> new AmplitudeScorer(code);
            case ComparatorConstants.TREND_STRENGTH_10 -> new TrendStrengthScorer(10, code);
            case ComparatorConstants.TREND_STRENGTH_20 -> new TrendStrengthScorer(20, code);
            default -> throw new IllegalArgumentException("不支持的打分器 code: " + code);
        };
    }
}
