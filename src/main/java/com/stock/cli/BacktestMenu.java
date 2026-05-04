package com.stock.cli;

import com.stock.backtest.strategy.StrategyRegistry;

import java.util.List;
import java.util.Scanner;

public class BacktestMenu {

    private final Scanner scanner;

    public BacktestMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void show() {
        System.out.println();
        System.out.println("  ══════════════ 回测引擎 ══════════════");
        System.out.println();

        List<String> strategyNames = StrategyRegistry.getStrategyNames();
        if (strategyNames.isEmpty()) {
            System.out.println("  当前没有已注册的短线策略。");
            System.out.println("  请在 StrategyRegistry 的 static 块中注册策略。");
            System.out.println();
            System.out.print("  按回车键返回主菜单...");
            scanner.nextLine();
            return;
        }

        System.out.println("  可用策略:");
        for (int i = 0; i < strategyNames.size(); i++) {
            System.out.println("    " + (i + 1) + ". " + strategyNames.get(i));
        }
        System.out.println();
        System.out.println("  回测引擎正在重构中，暂不可用。");
        System.out.print("  按回车键返回主菜单...");
        scanner.nextLine();
    }
}
