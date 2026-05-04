package com.stock.constants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 选择器常量定义。
 * <p>
 * 定义每个内置选择器的唯一 int 标识码，并提供 code → desc 的映射查询。
 * 编号规则：1xx=基础，2xx=成交量，3xx=涨跌幅，4xx=均线。
 */
public final class SelectorConstants {

    private SelectorConstants() {}

    // ======== 1xx: 基础 ========
    public static final int ALL_MAIN_BOARD = 1;

    // ======== 2xx: 成交量阈值 ========
    public static final int VOL_20_1P5 = 201;
    public static final int VOL_10_2P0 = 202;

    // ======== 3xx: 涨跌幅区间 ========
    public static final int CHANGE_M2_P10 = 301;
    public static final int CHANGE_M5_P10 = 302;

    // ======== 4xx: 均线多头 ========
    public static final int MA_5_20 = 401;
    public static final int MA_10_30 = 402;

    // ======== code → desc 映射 ========
    private static final Map<Integer, String> DESCS = new LinkedHashMap<>();

    static {
        DESCS.put(ALL_MAIN_BOARD, "全量主板");
        DESCS.put(VOL_20_1P5, "成交量筛选(20日均量 × 1.5倍)");
        DESCS.put(VOL_10_2P0, "成交量筛选(10日均量 × 2.0倍)");
        DESCS.put(CHANGE_M2_P10, "涨跌幅筛选(-2% ~ 10%)");
        DESCS.put(CHANGE_M5_P10, "涨跌幅筛选(-5% ~ 10%)");
        DESCS.put(MA_5_20, "均线多头(5日 > 20日)");
        DESCS.put(MA_10_30, "均线多头(10日 > 30日)");
    }

    /**
     * 根据 code 返回中文描述，未定义时返回 "未知(code)"。
     */
    public static String getDesc(int code) {
        return DESCS.getOrDefault(code, "未知(" + code + ")");
    }
}
