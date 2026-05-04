package com.stock.selector;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 责任链：将多个 StockSelector 按顺序串联，依次过滤。
 * <p>
 * 自身也实现 StockSelector，因此链可以嵌套或作为普通选择器使用。
 */
public class StockSelectorChain implements StockSelector {

    private final int code;
    private final String displayName;
    private final List<StockSelector> selectors;

    public StockSelectorChain(int code, String displayName, List<StockSelector> selectors) {
        this.code = code;
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.selectors = List.copyOf(selectors);
        if (selectors.isEmpty()) {
            throw new IllegalArgumentException("链必须包含至少一个选择器");
        }
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public List<String> select(List<String> candidates, LocalDate date) {
        List<String> result = new ArrayList<>(candidates);
        for (StockSelector selector : selectors) {
            result = selector.select(result, date);
        }
        return result;
    }

    /** 返回展示名称 */
    public String getDisplayName() {
        return displayName;
    }

    /** 返回链内选择器列表（只读） */
    public List<StockSelector> getSelectors() {
        return selectors;
    }
}
