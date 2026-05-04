package com.stock.comparator;

import java.time.LocalDate;
import java.util.*;

/**
 * 综合打分器——多个打分器加权求和。
 * <p>
 * 最终分数 = Σ(subScorer.score × weight)，分数归一化到 [0, 100]。
 */
public class CompositeScorer implements StockComparator {

    private final Map<StockComparator, Double> scorers; // scorer → weight
    private final int code;
    private final String desc;

    public CompositeScorer(int code, String desc, Map<StockComparator, Double> scorers) {
        double total = scorers.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 1.0) > 0.01)
            throw new IllegalArgumentException("权重之和必须为 1.0，当前=" + total);
        this.scorers = Map.copyOf(scorers);
        this.code = code;
        this.desc = Objects.requireNonNull(desc);
    }

    @Override
    public int getCode() { return code; }

    /** 返回综合打分器描述 */
    public String getDesc() { return desc; }

    @Override
    public Map<String, Double> score(List<String> codes, LocalDate date) {
        if (codes.isEmpty() || scorers.isEmpty()) return Collections.emptyMap();

        // 收集所有子打分器结果
        List<Map<String, Double>> allScores = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (var entry : scorers.entrySet()) {
            allScores.add(entry.getKey().score(codes, date));
            weights.add(entry.getValue());
        }

        // 加权求和 + 归一化
        Map<String, Double> result = new LinkedHashMap<>();
        for (String code : codes) {
            double total = 0;
            boolean hasScore = false;
            for (int i = 0; i < allScores.size(); i++) {
                Double s = allScores.get(i).get(code);
                if (s != null) {
                    total += s * weights.get(i);
                    hasScore = true;
                }
            }
            if (hasScore) result.put(code, total);
        }
        return result;
    }
}
