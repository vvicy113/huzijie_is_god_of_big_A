package com.stock.backtest.strategy;

import com.stock.comparator.*;
import com.stock.constants.ComparatorConstants;
import com.stock.constants.SelectorConstants;
import com.stock.model.TradeAction;
import com.stock.registry.DefaultSelectorRegistry;
import com.stock.selector.StockSelector;

import java.util.List;
import java.util.Map;

/**
 * 动量突破策略——第一个短线策略原型。
 * <p>
 * 选股：全量主板 → 20日放量1.5倍 → 均线多头(5,20)
 * 打分：动量10日(50%) + 量比20日(30%) + 均线乖离5日(20%)
 * 买卖：score > 60 且未持仓 → BUY；已持仓但不再符合选股条件 → SELL
 */
public class MomentumBreakoutStrategy implements Strategy {

    private static final String NAME = "动量突破";
    private static final double BUY_THRESHOLD = 60.0;

    private final StockSelector selector;
    private final StockComparator comparator;

    public MomentumBreakoutStrategy() {
        // 选股链
        DefaultSelectorRegistry reg = new DefaultSelectorRegistry("动量突破选股");
        reg.register(SelectorConstants.ALL_MAIN_BOARD);
        reg.register(SelectorConstants.VOL_20_1P5);
        reg.register(SelectorConstants.MA_5_20);
        this.selector = reg.build();

        // 综合打分
        this.comparator = new CompositeScorer(
                ComparatorConstants.COMPOSITE_MOM_VOL, "动量+量比+均线",
                Map.of(
                        new MomentumScorer(10, ComparatorConstants.MOMENTUM_10), 0.5,
                        new VolumeRatioScorer(20, ComparatorConstants.VOL_RATIO_20), 0.3,
                        new MaDistanceScorer(5, ComparatorConstants.MA_DIST_5), 0.2
                ));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public StockSelector getStockSelector() {
        return selector;
    }

    @Override
    public StockComparator getComparator() {
        return comparator;
    }

    @Override
    public void evaluate(StrategyContext ctx) {
        List<String> candidates = ctx.candidates();
        if (candidates.isEmpty()) return;

        // 批量打分
        Map<String, Double> scores = comparator.score(candidates, ctx.date());

        for (String code : candidates) {
            double score = scores.getOrDefault(code, 0.0);

            if (ctx.isHolding(code)) {
                // 已持仓 → 仍符合条件则 HOLD，否则 SELL
                if (score > BUY_THRESHOLD) {
                    ctx.signal(code, TradeAction.HOLD, score);
                } else {
                    ctx.signal(code, TradeAction.SELL, 0);
                }
            } else {
                // 未持仓 → 分数够高则 BUY
                if (score > BUY_THRESHOLD) {
                    ctx.signal(code, TradeAction.BUY, score);
                }
            }
        }
    }
}
