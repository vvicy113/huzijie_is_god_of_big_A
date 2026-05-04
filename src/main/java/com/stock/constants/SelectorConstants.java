package com.stock.constants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 选择器常量定义。
 * <p>
 * 定义每个内置选择器的唯一 int 标识码，并提供 code → desc 的映射查询。
 * 编号规则：1xx=基础，2xx=成交量，3xx=涨跌幅，4xx=均线，5xx=新高，
 * 6xx=连阳，7xx=振幅，8xx=均线乖离，9xx=跳空，10xx=成交额，11xx=缩量。
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

    // ======== 5xx: N日新高 ========
    public static final int NEW_HIGH_20 = 501;
    public static final int NEW_HIGH_60 = 502;

    // ======== 6xx: 连阳 ========
    public static final int CONSECUTIVE_RISE_3 = 601;
    public static final int CONSECUTIVE_RISE_5 = 602;

    // ======== 7xx: 振幅 ========
    public static final int AMPLITUDE_3_10 = 701;
    public static final int AMPLITUDE_5_15 = 702;

    // ======== 8xx: 均线乖离率 ========
    public static final int MA_DIST_5_P3 = 801;
    public static final int MA_DIST_20_P5 = 802;

    // ======== 9xx: 跳空高开 ========
    public static final int GAP_UP_2 = 901;

    // ======== 10xx: 成交额 ========
    public static final int AMOUNT_100M = 1001;
    public static final int AMOUNT_500M = 1002;

    // ======== 11xx: 缩量 ========
    public static final int SHRINK_VOL_20_0P5 = 1101;

    // ======== 12xx: 布林带 ========
    public static final int BOLL_LOWER_20_2 = 1201;
    public static final int BOLL_SQUEEZE_20_5 = 1202;

    // ======== 13xx: K线形态 ========
    public static final int LOWER_SHADOW_2 = 1301;

    // ======== 14xx: 相对位置 ========
    public static final int POS_RANK_LOW_20 = 1401;
    public static final int POS_RANK_HIGH_20 = 1402;

    // ======== 15xx: 动量 ========
    public static final int MOMENTUM_10_5_30 = 1501;
    public static final int MOMENTUM_20_M10_0 = 1502;

    // ======== 16xx: 爆量 ========
    public static final int VOL_SPIKE_3 = 1601;
    public static final int VOL_SPIKE_5 = 1602;

    // ======== 17xx: 金叉 ========
    public static final int MA_CROSS_5_20 = 1701;
    public static final int MA_CROSS_10_30 = 1702;

    // ======== 18xx: 上涨趋势 ========
    public static final int UP_TREND_20_60 = 1801;
    public static final int UP_TREND_10_70 = 1802;

    // ======== 19xx: 涨停 ========
    public static final int LIMIT_UP_9P5 = 1901;

    // ======== 20xx: K线实体 ========
    public static final int BODY_RATIO_0P5 = 2001;

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
        DESCS.put(NEW_HIGH_20, "20日新高");
        DESCS.put(NEW_HIGH_60, "60日新高");
        DESCS.put(CONSECUTIVE_RISE_3, "3连阳");
        DESCS.put(CONSECUTIVE_RISE_5, "5连阳");
        DESCS.put(AMPLITUDE_3_10, "振幅筛选(3% ~ 10%)");
        DESCS.put(AMPLITUDE_5_15, "振幅筛选(5% ~ 15%)");
        DESCS.put(MA_DIST_5_P3, "5日乖离率 < 3%");
        DESCS.put(MA_DIST_20_P5, "20日乖离率 < 5%");
        DESCS.put(GAP_UP_2, "跳空高开 > 2%");
        DESCS.put(AMOUNT_100M, "成交额 > 1亿");
        DESCS.put(AMOUNT_500M, "成交额 > 5亿");
        DESCS.put(SHRINK_VOL_20_0P5, "缩量(≤20日均量 × 0.5)");
        DESCS.put(BOLL_LOWER_20_2, "布林下轨(20日,2σ)");
        DESCS.put(BOLL_SQUEEZE_20_5, "布林收口(20日,带宽<5%)");
        DESCS.put(LOWER_SHADOW_2, "长下影线(≥实体×2)");
        DESCS.put(POS_RANK_LOW_20, "底部区域(20日位置0%~30%)");
        DESCS.put(POS_RANK_HIGH_20, "强势区域(20日位置70%~100%)");
        DESCS.put(MOMENTUM_10_5_30, "10日动量(5%~30%)");
        DESCS.put(MOMENTUM_20_M10_0, "20日超跌(-10%~0%)");
        DESCS.put(VOL_SPIKE_3, "单日爆量(>前日×3)");
        DESCS.put(VOL_SPIKE_5, "单日爆量(>前日×5)");
        DESCS.put(MA_CROSS_5_20, "均线金叉(5×20)");
        DESCS.put(MA_CROSS_10_30, "均线金叉(10×30)");
        DESCS.put(UP_TREND_20_60, "上涨趋势(20日中≥60%)");
        DESCS.put(UP_TREND_10_70, "上涨趋势(10日中≥70%)");
        DESCS.put(LIMIT_UP_9P5, "涨停板(≥9.5%)");
        DESCS.put(BODY_RATIO_0P5, "实体占比(≥50%)");
    }

    /**
     * 根据 code 返回中文描述，未定义时返回 "未知(code)"。
     */
    public static String getDesc(int code) {
        return DESCS.getOrDefault(code, "未知(" + code + ")");
    }
}
