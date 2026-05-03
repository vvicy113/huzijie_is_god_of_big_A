package com.stock.backtest.report;

import com.stock.model.BacktestResult;
import com.stock.model.PerformanceMetrics;
import com.stock.model.TradeRecord;

import static com.stock.backtest.report.ReportFormatter.*;

public class ReportGenerator {

    public String generate(BacktestResult result) {
        StringBuilder sb = new StringBuilder();
        PerformanceMetrics m = result.getMetrics();
        int w = 60;

        sb.append("\n").append(separator(w)).append("\n");
        sb.append("  回测报告\n");
        sb.append(separator(w)).append("\n");

        sb.append(formatRow("股票代码", result.getStockCode(), 18)).append("\n");
        sb.append(formatRow("策略名称", result.getStrategyName(), 18)).append("\n");
        sb.append(formatDate("回测区间",
                result.getEquityCurveDates().get(0),
                result.getEquityCurveDates().get(result.getEquityCurveDates().size() - 1)))
                .append("\n");

        sb.append(line(w)).append("\n");
        sb.append("  [收益指标]\n");
        sb.append(formatRow("总收益率", formatPct(m.getTotalReturn()), 18)).append("\n");
        sb.append(formatRow("年化收益率", formatPct(m.getAnnualizedReturn()), 18)).append("\n");
        sb.append(formatRow("最大回撤", formatPct(m.getMaxDrawdown()), 18)).append("\n");
        sb.append(formatRow("夏普比率", String.format("%.3f", m.getSharpeRatio()), 18)).append("\n");

        sb.append(line(w)).append("\n");
        sb.append("  [交易统计]\n");
        sb.append(formatRow("初始资金", formatMoney(m.getInitialCapital()), 18)).append("\n");
        sb.append(formatRow("最终资金", formatMoney(m.getFinalCapital()), 18)).append("\n");
        sb.append(formatRow("总交易次数", String.valueOf(m.getTotalTrades()), 18)).append("\n");
        sb.append(formatRow("盈利次数", String.valueOf(m.getWinningTrades()), 18)).append("\n");
        sb.append(formatRow("亏损次数", String.valueOf(m.getLosingTrades()), 18)).append("\n");
        sb.append(formatRow("胜率", formatPct(m.getWinRate()), 18)).append("\n");
        sb.append(formatRow("平均盈利", formatPct(m.getAvgProfit()), 18)).append("\n");
        sb.append(formatRow("平均亏损", formatPct(m.getAvgLoss()), 18)).append("\n");
        sb.append(formatRow("盈亏比", String.format("%.2f", m.getProfitLossRatio()), 18)).append("\n");

        if (!result.getTrades().isEmpty()) {
            sb.append(line(w)).append("\n");
            sb.append("  [交易明细]\n");
            sb.append(String.format("  %-3s %-10s %-8s %-10s %-8s %-8s %-6s\n",
                    "序号", "买入日期", "买入价", "卖出日期", "卖出价", "盈亏%", "持仓天"));
            sb.append(line(w)).append("\n");
            for (TradeRecord t : result.getTrades()) {
                sb.append(String.format("  %-3d %-10s %-8.2f %-10s %-8.2f %-+7.2f%% %-6d\n",
                        t.getTradeIndex(),
                        t.getBuyDate(),
                        t.getBuyPrice(),
                        t.getSellDate() != null ? t.getSellDate() : "持仓中",
                        t.getSellPrice(),
                        t.getProfitPercent(),
                        t.getHoldingDays()));
            }
        }

        sb.append(separator(w)).append("\n");
        return sb.toString();
    }
}
