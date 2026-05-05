package com.stock.backtest;

import com.stock.backtest.strategy.SignalBoard;
import com.stock.model.TradeAction;
import com.stock.model.TradeSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeSignalTest {

    @Test
    void shouldSortByScoreDescending() {
        TradeSignal a = new TradeSignal("600519", TradeAction.BUY, 85.0);
        TradeSignal b = new TradeSignal("000858", TradeAction.BUY, 92.0);
        TradeSignal c = new TradeSignal("000001", TradeAction.BUY, 85.0);

        assertTrue(b.compareTo(a) < 0);  // b(92) > a(85)
        assertTrue(a.compareTo(c) < 0);  // a(85) = c(85), a的code "600519" < "000001"? No
    }

    @Test
    void signalBoardShouldOrderByScore() {
        SignalBoard board = new SignalBoard();
        board.add(new TradeSignal("600519", TradeAction.BUY, 85.0));
        board.add(new TradeSignal("000858", TradeAction.BUY, 92.0));
        board.add(new TradeSignal("000001", TradeAction.BUY, 70.0));

        var all = board.all();
        assertEquals(3, all.size());
        assertTrue(all.get(0).score() >= all.get(1).score());
        assertTrue(all.get(1).score() >= all.get(2).score());
    }

    @Test
    void signalBoardRemove() {
        SignalBoard board = new SignalBoard();
        board.add(new TradeSignal("600519", TradeAction.BUY, 85.0));
        board.remove("600519");
        assertTrue(board.isEmpty());
    }

    @Test
    void signalBoardTopN() {
        SignalBoard board = new SignalBoard();
        board.add(new TradeSignal("a", TradeAction.BUY, 10));
        board.add(new TradeSignal("b", TradeAction.BUY, 50));
        board.add(new TradeSignal("c", TradeAction.BUY, 30));

        var top = board.topN(2);
        assertEquals(2, top.size());
        assertEquals("b", top.get(0).stockCode()); // highest
        assertEquals("c", top.get(1).stockCode()); // second
    }

    @Test
    void filterBuyShouldOnlyKeepBuys() {
        SignalBoard board = new SignalBoard();
        board.add(new TradeSignal("a", TradeAction.BUY, 85));
        board.add(new TradeSignal("b", TradeAction.SELL, 90));
        board.add(new TradeSignal("c", TradeAction.BUY, 70));

        var filtered = board.filterBuy(60);
        assertEquals(2, filtered.size());
        for (var s : filtered.all()) assertEquals(TradeAction.BUY, s.action());
    }
}
