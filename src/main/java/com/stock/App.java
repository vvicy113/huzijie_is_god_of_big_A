package com.stock;

import com.stock.cli.ConsoleUI;
import com.stock.log.LogSetup;

public class App {
    public static void main(String[] args) {
        LogSetup.init();
        new ConsoleUI().start();
    }
}
