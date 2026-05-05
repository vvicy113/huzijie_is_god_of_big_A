package com.stock.config;

import com.stock.constants.ComparatorConstants;
import com.stock.constants.SelectorConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量回测配置生成器。
 * <p>
 * 定义选股链、打分链、权重的分组预设，通过笛卡尔积自动生成全部组合。
 * 修改此类的分组定义即可调整组合空间。
 */
public class ConfigGeneratorUtils {

    // ======================== 选股器分组 ========================

    /** Layer 1: 基础（必须包含全量主板） */
    private static final int[][] SEL_BASE = {{SelectorConstants.ALL_MAIN_BOARD}};

    /** Layer 2: 量价过滤 */
    private static final int[][] SEL_VOLUME = {
            {SelectorConstants.VOL_20_1P5},           // 放量1.5倍
            {SelectorConstants.VOL_10_2P0},           // 放量2.0倍
            {SelectorConstants.CHANGE_M2_P10},        // 涨跌幅-2~10%
            {SelectorConstants.VOL_SPIKE_3},          // 爆量3倍
            {SelectorConstants.AMOUNT_100M},          // 成交额>1亿
    };

    /** Layer 3: 趋势确认 */
    private static final int[][] SEL_TREND = {
            {SelectorConstants.MA_5_20},              // 均线多头5,20
            {SelectorConstants.MA_10_30},             // 均线多头10,30
            {SelectorConstants.MA_CROSS_5_20},        // 金叉5,20
            {SelectorConstants.UP_TREND_20_60},       // 上涨趋势20日60%
    };

    // ======================== 打分器分组 ========================

    /** Layer 1: 动量打分 */
    private static final int[][] SCR_MOMENTUM = {
            {ComparatorConstants.MOMENTUM_10},
            {ComparatorConstants.MOMENTUM_20},
    };

    /** Layer 2: 活跃度打分 */
    private static final int[][] SCR_ACTIVITY = {
            {ComparatorConstants.VOL_RATIO_5},
            {ComparatorConstants.VOL_RATIO_20},
            {ComparatorConstants.AMPLITUDE},
    };

    /** Layer 3: 质量打分 */
    private static final int[][] SCR_QUALITY = {
            {ComparatorConstants.MA_DIST_5},
            {ComparatorConstants.MA_DIST_20},
            {ComparatorConstants.TREND_STRENGTH_10},
            {ComparatorConstants.TREND_STRENGTH_20},
    };

    /** 权重预设（动量/活跃/质量） */
    private static final double[][] WEIGHT_PRESETS = {
            {0.5, 0.3, 0.2},   // 标准
            {0.6, 0.25, 0.15}, // 偏动量
            {0.4, 0.3, 0.3},   // 均衡
    };

    // ======================== 其他参数 ========================

    private static final double[] BUY_THRESHOLDS = {50, 60, 70, 80};
    private static final int[] MAX_POSITIONS = {3, 5, 8, 10};
    private static final double[] CASH_RATIOS = {0.6, 0.8, 1.0};
    private static final double[] STOP_LOSS_PCTS = {-1, 5, 8, 10};
    private static final double[] TAKE_PROFIT_PCTS = {-1, 10, 20, 30};
    private static final int[] MAX_HOLDING_DAYS = {-1, 5, 10, 20};
    private static final boolean[] REPLACE_LOWER = {false, true};
    private static final boolean[] ALLOW_ADD = {false, true};

    // ======================== 生成全部组合 ========================

    public static List<BatchConfig> generateAll() {
        // Step 1: 选股链组合
        List<int[]> selectorCombos = cartesian(SEL_BASE, SEL_VOLUME, SEL_TREND);

        // Step 2: 打分链组合
        List<int[]> scorerCombos = cartesian(SCR_MOMENTUM, SCR_ACTIVITY, SCR_QUALITY);

        // Step 3: 全部笛卡尔积
        List<BatchConfig> result = new ArrayList<>();
        for (int[] sel : selectorCombos) {
            for (int[] scr : scorerCombos) {
                for (double[] weight : WEIGHT_PRESETS) {
                    for (double th : BUY_THRESHOLDS) {
                        for (int mp : MAX_POSITIONS) {
                            for (double cr : CASH_RATIOS) {
                                for (double sl : STOP_LOSS_PCTS) {
                                    for (double tp : TAKE_PROFIT_PCTS) {
                                        for (int hd : MAX_HOLDING_DAYS) {
                                            for (boolean rl : REPLACE_LOWER) {
                                                for (boolean aa : ALLOW_ADD) {
                                                    result.add(new BatchConfig(
                                                            sel, scr, weight,
                                                            th, mp, cr, sl, tp, hd, rl, aa));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    // ======================== 笛卡尔积工具 ========================

    /**
     * 多层笛卡尔积：每层取一个 int[]，拼接成最终选择器链。
     * 例如 combine({[1]}, {[201],[301]}, {[401],[1701]})
     * → {[1,201,401],[1,201,1701],[1,301,401],[1,301,1701]}
     */
    @SafeVarargs
    private static List<int[]> cartesian(int[][]... groups) {
        List<int[]> result = new ArrayList<>();
        cartesianRecurse(groups, 0, new int[0], result);
        return result;
    }

    private static void cartesianRecurse(int[][][] groups, int depth, int[] current, List<int[]> result) {
        if (depth >= groups.length) {
            result.add(current);
            return;
        }
        for (int[] choice : groups[depth]) {
            int[] next = new int[current.length + choice.length];
            System.arraycopy(current, 0, next, 0, current.length);
            System.arraycopy(choice, 0, next, current.length, choice.length);
            cartesianRecurse(groups, depth + 1, next, result);
        }
    }

    /** 返回预估总组合数 */
    public static int estimateCount() {
        return SEL_VOLUME.length * SEL_TREND.length                    // 选股
                * SCR_MOMENTUM.length * SCR_ACTIVITY.length * SCR_QUALITY.length // 打分
                * WEIGHT_PRESETS.length * BUY_THRESHOLDS.length
                * MAX_POSITIONS.length * CASH_RATIOS.length
                * STOP_LOSS_PCTS.length * TAKE_PROFIT_PCTS.length
                * MAX_HOLDING_DAYS.length * REPLACE_LOWER.length * ALLOW_ADD.length;
    }

    /** 打印组合空间信息 */
    public static String describeSpace() {
        return String.format(
                "选股链: %d种  打分链: %d种  权重: %d种  "
                        + "阈值: %d  持仓上限: %d  资金比: %d  "
                        + "止损: %d  止盈: %d  持仓天数: %d  "
                        + "换股: %d  加仓: %d  → 总计: %d组",
                SEL_VOLUME.length * SEL_TREND.length,
                SCR_MOMENTUM.length * SCR_ACTIVITY.length * SCR_QUALITY.length,
                WEIGHT_PRESETS.length,
                BUY_THRESHOLDS.length, MAX_POSITIONS.length, CASH_RATIOS.length,
                STOP_LOSS_PCTS.length, TAKE_PROFIT_PCTS.length, MAX_HOLDING_DAYS.length,
                REPLACE_LOWER.length, ALLOW_ADD.length,
                estimateCount());
    }
}
