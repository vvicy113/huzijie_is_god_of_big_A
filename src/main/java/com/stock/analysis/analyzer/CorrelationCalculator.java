package com.stock.analysis.analyzer;

import com.stock.model.IntradayTick;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.List;

public class CorrelationCalculator {

    /**
     * 计算两只股票分时价格序列的Pearson相关系数。
     * 以较短序列的长度对齐。
     *
     * @return 相关系数 [-1, 1]，数据不足返回 0
     */
    public double calculate(List<IntradayTick> ticks1, List<IntradayTick> ticks2) {
        int len = Math.min(ticks1.size(), ticks2.size());
        if (len < 5) return 0;

        double[] prices1 = new double[len];
        double[] prices2 = new double[len];
        for (int i = 0; i < len; i++) {
            prices1[i] = ticks1.get(i).getPrice();
            prices2[i] = ticks2.get(i).getPrice();
        }

        PearsonsCorrelation pc = new PearsonsCorrelation();
        double corr = pc.correlation(prices1, prices2);
        return Double.isNaN(corr) ? 0 : corr;
    }
}
