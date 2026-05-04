package com.stock.registry;

import com.stock.selector.StockSelector;

import java.util.List;

/**
 * 选择器注册表接口。
 * <p>
 * 管理多个 StockSelector 的注册，按注册顺序构建责任链。
 * 不同场景可创建不同的 Registry 实例，各自维护独立的选股器组合。
 * <p>
 * 典型用法：
 * <pre>
 *   StockSelectorRegistry reg = new DefaultSelectorRegistry("短线选股");
 *   reg.register(SelectorConstants.ALL_MAIN_BOARD);
 *   reg.register(SelectorConstants.CHANGE_M2_P10);
 *   StockSelector chain = reg.build();
 *   chain.select(candidates, date);
 * </pre>
 */
public interface StockSelectorRegistry {

    /**
     * 按常量码注册选择器。code 必须能在工厂中创建对应的实例。
     * 注册顺序决定链的执行顺序。
     */
    void register(int code);

    /**
     * 直接注册选择器实例。用于注册自定义实现或工厂不支持的 code。
     */
    void register(StockSelector selector);

    /**
     * 构建责任链。返回的 StockSelector 按注册顺序依次执行过滤。
     */
    StockSelector build();

    /**
     * 返回已注册的 code 列表（按注册顺序）。
     */
    List<Integer> codes();
}
