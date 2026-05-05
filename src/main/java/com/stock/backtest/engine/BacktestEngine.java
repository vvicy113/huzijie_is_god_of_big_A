package com.stock.backtest.engine;

import com.stock.backtest.executor.DoubleRef;
import com.stock.backtest.executor.EqualSplitExecutor;
import com.stock.backtest.executor.TradeExecutor;
import com.stock.backtest.loader.CsvDataLoader;
import com.stock.backtest.metrics.MetricsCalculator;
import com.stock.backtest.strategy.*;
import com.stock.config.BacktestConfig;
import com.stock.db.KLineRepository;
import com.stock.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 * 短线多股票回测引擎——整个系统的核心编排器。
 *
 * <h3>职责</h3>
 * Engine 只管<b>编排流程</b>，不关心策略逻辑：
 * <ol>
 *   <li>获取交易日历</li>
 *   <li>每天：调用 Strategy 的选股器筛选候选</li>
 *   <li>每天：加载候选股票的历史日K线</li>
 *   <li>每天：调用 Strategy.evaluate() 生成交易信号</li>
 *   <li>每天：风控检查（止损/止盈/持仓天数）</li>
 *   <li>每天：调用 TradeExecutor 执行买卖</li>
 *   <li>每天：计算权益曲线（现金 + 持仓市值）</li>
 *   <li>回测结束：强制清仓 + 计算绩效指标 + 生成报告</li>
 * </ol>
 *
 * <h3>可替换组件</h3>
 * <ul>
 *   <li>{@link Strategy} — 通过构造函数的 TradeExecutor 参数注入</li>
 *   <li>{@link TradeExecutor} — 通过 {@link #BacktestEngine(BacktestConfig, TradeExecutor)} 注入</li>
 *   <li>{@link BacktestConfig} — 所有参数通过 Builder 配置</li>
 * </ul>
 *
 * <h3>数据存储</h3>
 * 回测过程中的状态用普通 Map 管理，不依赖外部存储：
 * <ul>
 *   <li>{@code Map<String, Position> positions} — 当前持仓，key=stockCode</li>
 *   <li>{@code Map<String, LocalDate> buyDates} — 每只股票的买入日期，用于计算持仓天数</li>
 *   <li>{@code DoubleRef cash} — 可变现金引用，TradeExecutor 直接修改</li>
 *   <li>{@code List<TradeRecord> allTrades} — 全部交易记录</li>
 *   <li>{@code List<Double> equityValues} — 每日权益值，用于计算收益曲线</li>
 * </ul>
 *
 * @see Strategy
 * @see TradeExecutor
 * @see BacktestConfig
 */
public class BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngine.class);

    /** 日K线数据加载器 */
    private final CsvDataLoader loader;
    /** 数据库查询仓库 */
    private final KLineRepository repo;
    /** 交易执行器 */
    private final TradeExecutor executor;
    /** 回测配置（所有参数集中管理） */
    private final BacktestConfig config;

    // ======================== 构造函数 ========================

    /** 使用默认配置 + 默认等分资金执行器 */
    public BacktestEngine() {
        this(BacktestConfig.defaults());
    }

    /**
     * 使用指定配置 + 默认等分资金执行器。
     * @param config 回测配置，内含佣金率、风控阈值、仓位限制等
     */
    public BacktestEngine(BacktestConfig config) {
        this.config = config;
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
        this.executor = new EqualSplitExecutor(config);
    }

    /**
     * 使用指定配置 + 自定义执行器。
     * @param config   回测配置
     * @param executor 自定义交易执行器（如全仓一股、按比例分配等）
     */
    public BacktestEngine(BacktestConfig config, TradeExecutor executor) {
        this.config = config;
        this.loader = new CsvDataLoader();
        this.repo = new KLineRepository();
        this.executor = executor;
    }

    // ======================== 公共入口 ========================

    /**
     * Dry Run 模式：只生成信号不执行真实买卖。
     * 用于快速验证策略的信号生成逻辑，不产生损益。
     */
    public void dryRun(Strategy strategy, LocalDate from, LocalDate to) {
        log.info("启动 Dry Run — 仅展示信号，不执行交易");
        backtest(strategy, from, to, true);
    }

    /**
     * 正式回测：真实执行买卖，计算收益。
     *
     * @param strategy 交易策略（含选股器 + 评估逻辑）
     * @param from     回测起始日期（含）
     * @param to       回测结束日期（含）
     * @return 回测结果（含绩效指标、交易记录、权益曲线）
     */
    public BacktestResult run(Strategy strategy, LocalDate from, LocalDate to) {
        log.info("启动正式回测 — 真实执行买卖");
        return backtest(strategy, from, to, false);
    }

    // ======================== 核心回测循环 ========================

    /**
     * 核心回测方法，按交易日逐日执行完整流程。
     *
     * @param strategy 策略
     * @param from     起始日期
     * @param to       结束日期
     * @param dry      true=仅展示信号 / false=真实交易
     * @return 回测结果
     */
    private BacktestResult backtest(Strategy strategy, LocalDate from, LocalDate to, boolean dry) {
        // ---- 初始化 ----
        strategy.onReset();
        log.info("策略 onReset 完成");

        List<LocalDate> dates = getTradingDates(from, to);
        if (dates.isEmpty()) {
            log.warn("指定日期范围内无交易日数据: {} ~ {}", from, to);
            return null;
        }

        // 状态容器
        DoubleRef cash = new DoubleRef(config.initialCapital());         // 可变现金
        Map<String, Position> positions = new HashMap<>();               // 持仓
        Map<String, LocalDate> buyDates = new HashMap<>();               // 买入日期
        Map<String, Double> lastPrices = new HashMap<>();                // 最后一日的收盘价（用于收盘清仓）
        List<TradeRecord> allTrades = new ArrayList<>();                 // 全部成交记录
        List<Double> equityValues = new ArrayList<>();                   // 权益曲线 Y 轴
        List<LocalDate> equityDates = new ArrayList<>();                 // 权益曲线 X 轴

        log.info("策略: {}  执行器: {}  日期: {} ~ {}  交易日数: {}  初始资金: {}",
                strategy.getName(), executor.getName(), from, to, dates.size(), (int) config.initialCapital());

        // ---- 逐日循环 ----
        int skippedDays = 0;
        for (LocalDate date : dates) {

            // ====== 步骤1: 选股 ======
            // 从全市场按策略的选股链过滤出候选池
            List<String> candidates = strategy.getStockSelector().select(List.of(), date);
            if (candidates.isEmpty()) {
                skippedDays++;
                continue;
            }

            // ====== 步骤2: 加载日K线 + 提取当日收盘价 ======
            // 每只候选股票加载最近60个交易日的K线用于技术指标计算
            Map<String, List<KLine>> klineMap = new LinkedHashMap<>();
            Map<String, Double> prices = new LinkedHashMap<>();
            int loadedCount = 0;
            for (String code : candidates) {
                try {
                    List<KLine> klines = loader.loadDailyKLine(code, date.minusDays(60), date);
                    if (!klines.isEmpty()) {
                        klineMap.put(code, klines);
                        // 当日收盘价 = 加载的K线列表中最后一条
                        prices.put(code, klines.get(klines.size() - 1).getClose());
                        loadedCount++;
                    }
                } catch (IOException e) {
                    log.debug("加载K线失败: {}", code);
                }
            }
            if (klineMap.isEmpty()) continue;

            // ====== 步骤3: 策略评估 ======
            // 策略根据K线数据和当前持仓，往 SignalBoard 填入交易信号
            SignalBoard board = new SignalBoard();
            StrategyContext ctx = new StrategyContext(candidates, klineMap, positions, cash.get(), date, board);
            strategy.evaluate(ctx);

            // ====== 步骤4: 风控检查 ======
            // 对已有持仓进行止损/止盈/持仓天数检查，触发则追加 SELL 信号
            applyRiskCheck(positions, buyDates, prices, date, board);

            // 信号为空 → 跳过执行，但仍需记录权益
            if (board.isEmpty()) {
                equityDates.add(date);
                equityValues.add(cash.get() + positions.values().stream()
                        .mapToDouble(p -> p.getShares() * p.getAvgCost()).sum());
                continue;
            }

            // ====== 步骤5: 执行交易 ======
            List<TradeRecord> dayTrades;
            if (dry) {
                // Dry Run：只模拟持仓变化，不动真实资金
                dayTrades = List.of();
                for (TradeSignal s : board.all()) {
                    if (s.action() == TradeAction.BUY) {
                        positions.put(s.stockCode(), Position.builder()
                                .holding(true).shares(100).avgCost(0).totalCost(0).currentValue(0).build());
                        buyDates.put(s.stockCode(), date);
                    } else if (s.action() == TradeAction.SELL) {
                        positions.remove(s.stockCode());
                        buyDates.remove(s.stockCode());
                    }
                }
            } else {
                // 真实模式：TradeExecutor 按信号执行买卖
                int posBefore = positions.size();
                double cashBefore = cash.get();
                dayTrades = executor.execute(board, positions, cash, prices, date);
                allTrades.addAll(dayTrades);

                // 同步买入日期（用于后续风控计算持仓天数）
                for (TradeSignal s : board.all()) {
                    if (s.action() == TradeAction.BUY) buyDates.put(s.stockCode(), date);
                    else if (s.action() == TradeAction.SELL) buyDates.remove(s.stockCode());
                }

                log.debug("  执行: 成交{}笔, 持仓{}→{}, 资金{}→{}",
                        dayTrades.size(), posBefore, positions.size(), (int) cashBefore, (int) cash.get());
            }

            // ====== 步骤6: 计算权益 ======
            // 权益 = 现金 + Σ(持仓股数 × 当日收盘价)
            double totalEquity = cash.get();
            for (var entry : positions.entrySet()) {
                String code = entry.getKey();
                Position p = entry.getValue();
                Double close = prices.get(code);
                // 如果没有当日价格（候选被筛掉），用成本价兜底
                totalEquity += p.getShares() * (close != null ? close : p.getAvgCost());
            }
            equityDates.add(date);
            equityValues.add(totalEquity);

            // ====== 步骤7: 每日日志 ======
            int buys = 0, sells = 0;
            for (TradeSignal s : board.all()) {
                if (s.action() == TradeAction.BUY) buys++;
                if (s.action() == TradeAction.SELL) sells++;
            }
            double marketValue = totalEquity - cash.get();
            log.info("{}  候选:{}  买:{}  卖:{}  持仓:{}  资金:{}  市值:{}  总:{}",
                    date, candidates.size(), buys, sells, positions.size(),
                    (int) cash.get(), (int) marketValue, (int) totalEquity);

            // 保存当日价格，用于回测结束时强制清仓
            lastPrices = new HashMap<>(prices);
        }

        log.info("逐日循环结束，有效交易日: {}, 跳过: {} (无候选)",
                dates.size() - skippedDays, skippedDays);

        // ====== 步骤8: 收盘强制清仓 ======
        // 回测结束时按最后一日收盘价卖出所有持仓
        if (!positions.isEmpty() && config.forceCloseAtEnd() && !lastPrices.isEmpty()) {
            log.info("收盘强制清仓: {} 只股票", positions.size());
            double commRate = config.commissionRate();
            double minComm = config.minCommission();
            double taxRate = config.stampTaxRate();
            double totalCloseRevenue = 0;

            for (var it = positions.entrySet().iterator(); it.hasNext(); ) {
                var entry = it.next();
                String code = entry.getKey();
                Position p = entry.getValue();
                Double close = lastPrices.get(code);
                if (close == null) close = p.getAvgCost();

                double revenue = p.getShares() * close;
                double commission = Math.max(revenue * commRate, minComm);
                double stampTax = revenue * taxRate;
                double net = revenue - commission - stampTax;
                cash.add(net);
                totalCloseRevenue += net;

                LocalDate buyDt = buyDates.get(code);
                long holdDays = buyDt != null ? dates.get(dates.size() - 1).toEpochDay() - buyDt.toEpochDay() : 0;

                allTrades.add(TradeRecord.builder()
                        .tradeIndex(allTrades.size() + 1)
                        .buyDate(buyDt).buyPrice(p.getAvgCost()).buyShares(p.getShares())
                        .sellDate(dates.get(dates.size() - 1)).sellPrice(close)
                        .sellCommission(commission + stampTax)
                        .profit(net - p.getTotalCost())
                        .profitPercent((net - p.getTotalCost()) / p.getTotalCost() * 100)
                        .holdingDays((int) holdDays).build());
                it.remove();

                log.debug("  清仓 {}: {}股 @{}  盈亏:{:.1f}%  持仓{}天",
                        code, p.getShares(), close,
                        (net - p.getTotalCost()) / p.getTotalCost() * 100, holdDays);
            }
            log.info("清仓完毕，回笼资金: {}", (int) totalCloseRevenue);
        }

        // ====== 步骤9: 最终汇总 ======
        log.info("最终资金: {}  交易次数: {} (买入{} + 清仓)",
                (int) cash.get(), allTrades.size(),
                (int) allTrades.stream().filter(t -> t.getBuyDate() != null).count());

        var metrics = MetricsCalculator.calculate(config.initialCapital(), cash.get(),
                allTrades, equityValues, equityDates, config.riskFreeRate());

        return BacktestResult.builder()
                .stockCode("多股票").strategyName(strategy.getName())
                .metrics(metrics).trades(allTrades)
                .equityCurveDates(equityDates).equityCurveValues(equityValues)
                .build();
    }

    // ======================== 风控检查 ========================

    /**
     * 风控检查：遍历当前所有持仓，检查是否触发止损/止盈/超期。
     * <p>
     * 风控是<b>硬规则</b>，不依赖策略判断。触发后直接往 board 追加 SELL 信号，
     * 与策略主动 SELL 合并后统一由 TradeExecutor 执行。
     * <p>
     * 检查优先级：止损 > 止盈 > 持仓天数（任一触发即停止检查后续项）
     *
     * @param positions 当前持仓
     * @param buyDates  每只股票的买入日期
     * @param prices    当日收盘价 Map
     * @param date      当前交易日
     * @param board     信号板（风控 SELL 信号追加到此）
     */
    private void applyRiskCheck(Map<String, Position> positions, Map<String, LocalDate> buyDates,
                                Map<String, Double> prices, LocalDate date, SignalBoard board) {
        // 三种风控都未启用则快速返回
        if (!config.stopLossEnabled() && !config.takeProfitEnabled() && !config.maxHoldingDaysEnabled())
            return;

        for (var entry : positions.entrySet()) {
            String code = entry.getKey();
            Position pos = entry.getValue();
            Double close = prices.get(code);
            if (close == null || close <= 0) continue;

            LocalDate buyDate = buyDates.get(code);
            boolean sell = false;
            String reason = "";

            // 止损：亏损幅度超过阈值
            if (config.stopLossEnabled()) {
                double loss = (pos.getAvgCost() - close) / pos.getAvgCost() * 100;
                if (loss >= config.stopLossPct()) {
                    sell = true;
                    reason = String.format("止损(成本%.2f 现价%.2f 亏损%.1f%%)",
                            pos.getAvgCost(), close, loss);
                }
            }

            // 止盈：盈利幅度超过阈值
            if (!sell && config.takeProfitEnabled()) {
                double profit = (close - pos.getAvgCost()) / pos.getAvgCost() * 100;
                if (profit >= config.takeProfitPct()) {
                    sell = true;
                    reason = String.format("止盈(成本%.2f 现价%.2f 盈利%.1f%%)",
                            pos.getAvgCost(), close, profit);
                }
            }

            // 持仓天数超限
            if (!sell && config.maxHoldingDaysEnabled() && buyDate != null) {
                long days = date.toEpochDay() - buyDate.toEpochDay();
                if (days >= config.maxHoldingDays()) {
                    sell = true;
                    reason = String.format("持仓超期(%d天 >= %d天)", days, config.maxHoldingDays());
                }
            }

            if (sell) {
                log.info("  风控卖出: {}  原因: {}", code, reason);
                // score=-1 表示风控强制卖出（与策略主动卖出区分）
                board.add(new TradeSignal(code, TradeAction.SELL, -1));
            }
        }
    }

    // ======================== 交易日历 ========================

    /**
     * 获取日期范围内的所有交易日。
     * <p>
     * 取数据库中任意一只股票（第一只）的K线日期作为交易日历。
     * 假设所有股票的交易日一致（A股市场确实如此，除停牌外）。
     *
     * @param from 起始日期
     * @param to   结束日期
     * @return 交易日列表（按日期升序）
     */
    private List<LocalDate> getTradingDates(LocalDate from, LocalDate to) {
        List<LocalDate> dates = new ArrayList<>();
        List<String> allCodes = repo.getAllStockCodes();
        if (allCodes.isEmpty()) {
            log.warn("数据库中无股票数据，无法获取交易日历");
            return dates;
        }
        // 取第一只股票作为交易日历基准
        String refCode = allCodes.get(0);
        List<KLine> klines = repo.findByStockCode(refCode, from, to);
        for (KLine k : klines) dates.add(k.getDate());
        log.debug("交易日历基准: {}, {} ~ {} 共{}天", refCode, from, to, dates.size());
        return dates;
    }
}
