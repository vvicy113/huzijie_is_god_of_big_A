package com.stock.comparator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 股票打分器接口——对候选池批量打分。
 * <p>
 * Strategy 用 Selector 选出候选池后，用 Comparator 对候选逐一评分，
 * 引擎按分数从高到低分配资金。
 */
public interface StockComparator {

    /** 返回唯一标识码，与 {@link com.stock.constants.ComparatorConstants} 对应 */
    int getCode();

    /** 返回人类可读的描述文本 */
    String getDesc();

    /**
     * 对候选股票列表批量打分。
     *
     * @param codes 候选股票代码列表
     * @param date  目标交易日
     * @return code → score 的映射，score >= 0，越高越强
     */
    Map<String, Double> score(List<String> codes, LocalDate date);
}
