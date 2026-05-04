package com.stock.registry;

import com.stock.constants.SelectorConstants;
import com.stock.selector.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 默认选择器注册表实现。
 * <p>
 * 内部维护一个有序的选择器列表，通过 {@link #build()} 构建 {@link StockSelectorChain}。
 * 工厂方法 {@link #createFromCode(int)} 可被子类覆盖，以支持自定义选择器。
 */
public class DefaultSelectorRegistry implements StockSelectorRegistry {

    private final String name;
    private final List<StockSelector> selectors;

    public DefaultSelectorRegistry(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.selectors = new ArrayList<>();
    }

    @Override
    public void register(int code) {
        StockSelector s = createFromCode(code);
        if (s == null) {
            throw new IllegalArgumentException("不支持的选择器代码: " + code
                    + " (" + SelectorConstants.getDesc(code) + ")，请使用 register(StockSelector) 注册自定义实例");
        }
        selectors.add(s);
    }

    @Override
    public void register(StockSelector selector) {
        selectors.add(Objects.requireNonNull(selector, "selector"));
    }

    @Override
    public StockSelector build() {
        if (selectors.isEmpty()) {
            throw new IllegalStateException("注册表为空，至少需要注册一个选择器");
        }
        return new StockSelectorChain(
                selectors.size() == 1 ? selectors.get(0).getCode() : -1,
                name,
                List.copyOf(selectors));
    }

    @Override
    public List<Integer> codes() {
        return selectors.stream().map(StockSelector::getCode).toList();
    }

    /**
     * 根据 code 创建对应的选择器实例。
     * <p>
     * 子类可覆盖此方法以扩展自定义选择器：
     * <pre>
     *   class MyRegistry extends DefaultSelectorRegistry {
     *     protected StockSelector createFromCode(int code) {
     *         return switch (code) {
     *             case 999 -&gt; new MyCustomSelector();
     *             default -&gt; super.createFromCode(code);
     *         };
     *     }
     *   }
     * </pre>
     */
    protected StockSelector createFromCode(int code) {
        return switch (code) {
            case SelectorConstants.ALL_MAIN_BOARD     -> new AllMainBoardSelector();
            case SelectorConstants.VOL_20_1P5          -> new VolumeThresholdSelector(20, 1.5, code);
            case SelectorConstants.VOL_10_2P0          -> new VolumeThresholdSelector(10, 2.0, code);
            case SelectorConstants.CHANGE_M2_P10       -> new PriceChangeSelector(-2.0, 9.98, code);
            case SelectorConstants.CHANGE_M5_P10       -> new PriceChangeSelector(-5.0, 10.0, code);
            case SelectorConstants.MA_5_20             -> new MaAlignmentSelector(5, 20, code);
            case SelectorConstants.MA_10_30            -> new MaAlignmentSelector(10, 30, code);
            case SelectorConstants.NEW_HIGH_20         -> new NewHighSelector(20, code);
            case SelectorConstants.NEW_HIGH_60         -> new NewHighSelector(60, code);
            case SelectorConstants.CONSECUTIVE_RISE_3  -> new ConsecutiveRiseSelector(3, code);
            case SelectorConstants.CONSECUTIVE_RISE_5  -> new ConsecutiveRiseSelector(5, code);
            case SelectorConstants.AMPLITUDE_3_10      -> new AmplitudeSelector(3.0, 10.0, code);
            case SelectorConstants.AMPLITUDE_5_15      -> new AmplitudeSelector(5.0, 15.0, code);
            case SelectorConstants.MA_DIST_5_P3        -> new MaDistanceSelector(5, 3.0, code);
            case SelectorConstants.MA_DIST_20_P5       -> new MaDistanceSelector(20, 5.0, code);
            case SelectorConstants.GAP_UP_2            -> new GapUpSelector(2.0, code);
            case SelectorConstants.AMOUNT_100M         -> new AmountThresholdSelector(100_000_000, code);
            case SelectorConstants.AMOUNT_500M         -> new AmountThresholdSelector(500_000_000, code);
            case SelectorConstants.SHRINK_VOL_20_0P5   -> new ShrinkVolumeSelector(20, 0.5, code);
            case SelectorConstants.BOLL_LOWER_20_2     -> new BollingerLowerSelector(20, 2.0, code);
            case SelectorConstants.BOLL_SQUEEZE_20_5   -> new BollingerSqueezeSelector(20, 2.0, 5.0, code);
            case SelectorConstants.LOWER_SHADOW_2      -> new LowerShadowSelector(2.0, code);
            case SelectorConstants.POS_RANK_LOW_20     -> new PositionRankSelector(20, 0, 30, code);
            case SelectorConstants.POS_RANK_HIGH_20    -> new PositionRankSelector(20, 70, 100, code);
            case SelectorConstants.MOMENTUM_10_5_30    -> new MomentumSelector(10, 5.0, 30.0, code);
            case SelectorConstants.MOMENTUM_20_M10_0   -> new MomentumSelector(20, -10.0, 0.0, code);
            case SelectorConstants.VOL_SPIKE_3         -> new VolumeSpikeSelector(3.0, code);
            case SelectorConstants.VOL_SPIKE_5         -> new VolumeSpikeSelector(5.0, code);
            case SelectorConstants.MA_CROSS_5_20       -> new MaGoldenCrossSelector(5, 20, code);
            case SelectorConstants.MA_CROSS_10_30      -> new MaGoldenCrossSelector(10, 30, code);
            case SelectorConstants.UP_TREND_20_60      -> new UpTrendRatioSelector(20, 0.6, code);
            case SelectorConstants.UP_TREND_10_70      -> new UpTrendRatioSelector(10, 0.7, code);
            case SelectorConstants.LIMIT_UP_9P5        -> new LimitUpSelector(9.5, code);
            case SelectorConstants.BODY_RATIO_0P5      -> new BodyRatioSelector(0.5, code);
            default -> null;
        };
    }

    /** 返回注册表名称 */
    public String getName() {
        return name;
    }

    /** 返回当前已注册的选择器数量 */
    public int size() {
        return selectors.size();
    }
}
