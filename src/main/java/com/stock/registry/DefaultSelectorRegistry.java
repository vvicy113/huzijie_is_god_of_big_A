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
            case SelectorConstants.ALL_MAIN_BOARD -> new AllMainBoardSelector();
            case SelectorConstants.VOL_20_1P5  -> new VolumeThresholdSelector(20, 1.5, code);
            case SelectorConstants.VOL_10_2P0  -> new VolumeThresholdSelector(10, 2.0, code);
            case SelectorConstants.CHANGE_M2_P10 -> new PriceChangeSelector(-2.0, 9.98, code);
            case SelectorConstants.CHANGE_M5_P10 -> new PriceChangeSelector(-5.0, 10.0, code);
            case SelectorConstants.MA_5_20  -> new MaAlignmentSelector(5, 20, code);
            case SelectorConstants.MA_10_30 -> new MaAlignmentSelector(10, 30, code);
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
