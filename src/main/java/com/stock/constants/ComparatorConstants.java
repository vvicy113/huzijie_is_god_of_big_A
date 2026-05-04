package com.stock.constants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 打分器常量定义。
 * <p>
 * 编号规则：5xx=动量，6xx=量比，7xx=均线乖离，8xx=振幅，9xx=综合。
 */
public final class ComparatorConstants {

    private ComparatorConstants() {}

    // ======== 5xx: 动量 ========
    public static final int MOMENTUM_10 = 501;
    public static final int MOMENTUM_20 = 502;

    // ======== 6xx: 量比 ========
    public static final int VOL_RATIO_5 = 601;
    public static final int VOL_RATIO_20 = 602;

    // ======== 7xx: 均线乖离 ========
    public static final int MA_DIST_5 = 701;
    public static final int MA_DIST_20 = 702;

    // ======== 8xx: 振幅 ========
    public static final int AMPLITUDE = 801;

    // ======== 9xx: 综合 ========
    public static final int COMPOSITE_MOM_VOL = 901;

    // ======== code → desc 映射 ========
    private static final Map<Integer, String> DESCS = new LinkedHashMap<>();

    static {
        DESCS.put(MOMENTUM_10, "10日动量打分");
        DESCS.put(MOMENTUM_20, "20日动量打分");
        DESCS.put(VOL_RATIO_5, "5日量比打分");
        DESCS.put(VOL_RATIO_20, "20日量比打分");
        DESCS.put(MA_DIST_5, "5日乖离打分");
        DESCS.put(MA_DIST_20, "20日乖离打分");
        DESCS.put(AMPLITUDE, "振幅打分");
        DESCS.put(COMPOSITE_MOM_VOL, "综合打分(动量+量比)");
    }

    public static String getDesc(int code) {
        return DESCS.getOrDefault(code, "未知(" + code + ")");
    }
}
