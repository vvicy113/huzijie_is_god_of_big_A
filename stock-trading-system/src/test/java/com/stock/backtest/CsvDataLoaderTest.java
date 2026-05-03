package com.stock.backtest;

import com.stock.backtest.loader.CsvDataLoader;
import com.stock.model.KLine;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvDataLoaderTest {

    @Test
    void shouldParseValidCsv() throws IOException {
        String csv = """
                日期,开盘价,最高价,最低价,收盘价,成交量,成交额
                2024-01-02,10.50,10.80,10.30,10.60,1000000,10500000.00
                2024-01-03,10.60,10.90,10.40,10.80,1200000,12800000.00
                2024-01-04,10.70,11.00,10.60,10.90,1100000,11900000.00
                """;
        Path tmp = Files.createTempFile("test", ".csv");
        Files.writeString(tmp, csv);

        CsvDataLoader loader = new CsvDataLoader();
        List<KLine> result = loader.load(tmp.toString());

        assertEquals(3, result.size());
        assertEquals(10.50, result.get(0).getOpen());
        assertEquals(10.80, result.get(1).getClose());
        assertEquals(1200000, result.get(1).getVolume());
        Files.deleteIfExists(tmp);
    }

    @Test
    void shouldRejectUnsortedData() throws IOException {
        String csv = """
                日期,开盘价,最高价,最低价,收盘价,成交量,成交额
                2024-01-04,10.70,11.00,10.60,10.90,1100000,11900000.00
                2024-01-02,10.50,10.80,10.30,10.60,1000000,10500000.00
                """;
        Path tmp = Files.createTempFile("test", ".csv");
        Files.writeString(tmp, csv);

        CsvDataLoader loader = new CsvDataLoader();
        assertThrows(IllegalArgumentException.class, () -> loader.load(tmp.toString()));
        Files.deleteIfExists(tmp);
    }
}
