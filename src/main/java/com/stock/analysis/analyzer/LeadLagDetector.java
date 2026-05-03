package com.stock.analysis.analyzer;

import com.stock.model.IntradayTick;
import com.stock.model.LeadLagResult;

import java.util.List;

/**
 * 领涨跟风检测：通过互相关分析，检测两只股票的价格变动是否存在时间领先关系。
 */
public class LeadLagDetector {

    /**
     * 检测 stock1 是否领先 stock2。
     * 通过计算不同偏移下的相关系数，寻找最大相关性的偏移量。
     *
     * @param code1   股票1代码
     * @param code2   股票2代码
     * @param ticks1  股票1分时数据
     * @param ticks2  股票2分时数据
     * @param maxLag  最大偏移（分钟）
     * @return 领涨跟风结果
     */
    public LeadLagResult detect(String code1, String code2,
                                 List<IntradayTick> ticks1, List<IntradayTick> ticks2,
                                 int maxLag) {
        int len = Math.min(ticks1.size(), ticks2.size());
        if (len < maxLag + 5) {
            return LeadLagResult.builder()
                    .leaderCode(code1).followerCode(code2)
                    .leadMinutes(0).confidence(0).build();
        }

        double[] prices1 = new double[len];
        double[] prices2 = new double[len];
        for (int i = 0; i < len; i++) {
            prices1[i] = ticks1.get(i).getPrice();
            prices2[i] = ticks2.get(i).getPrice();
        }

        // 计算returns序列
        double[] returns1 = new double[len - 1];
        double[] returns2 = new double[len - 1];
        for (int i = 1; i < len; i++) {
            returns1[i - 1] = prices1[i] - prices1[i - 1];
            returns2[i - 1] = prices2[i] - prices2[i - 1];
        }

        int bestLag = 0;
        double bestCorr = 0;

        // 检测 stock1 领先 stock2 的 lag
        // lag > 0: stock1领先stock2 (stock2的return与stock1的滞后return相关)
        for (int lag = -maxLag; lag <= maxLag; lag++) {
            int start1 = Math.max(0, lag);
            int start2 = Math.max(0, -lag);
            int count = Math.min(returns1.length - start1, returns2.length - start2);
            if (count < 5) continue;

            double sum1 = 0, sum2 = 0;
            for (int i = 0; i < count; i++) {
                sum1 += returns1[start1 + i];
                sum2 += returns2[start2 + i];
            }
            double mean1 = sum1 / count;
            double mean2 = sum2 / count;

            double cov = 0, var1 = 0, var2 = 0;
            for (int i = 0; i < count; i++) {
                double d1 = returns1[start1 + i] - mean1;
                double d2 = returns2[start2 + i] - mean2;
                cov += d1 * d2;
                var1 += d1 * d1;
                var2 += d2 * d2;
            }
            double corr = (var1 > 0 && var2 > 0) ? cov / Math.sqrt(var1 * var2) : 0;

            if (Math.abs(corr) > Math.abs(bestCorr)) {
                bestCorr = corr;
                bestLag = lag;
            }
        }

        String leader = bestLag > 0 ? code1 : code2;
        String follower = bestLag > 0 ? code2 : code1;
        int leadMinutes = Math.abs(bestLag);

        return LeadLagResult.builder()
                .leaderCode(leader)
                .followerCode(follower)
                .leadMinutes(leadMinutes)
                .confidence(bestCorr)
                .build();
    }
}
