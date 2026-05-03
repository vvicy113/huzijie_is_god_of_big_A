package com.stock.backtest.strategy;

import com.stock.model.KLine;
import com.stock.model.Position;
import com.stock.model.TradeAction;

import java.util.List;

public interface Strategy {
    String getName();
    TradeAction evaluate(int currentIndex, List<KLine> history, Position currentPosition);
    default void onReset() {}
}
