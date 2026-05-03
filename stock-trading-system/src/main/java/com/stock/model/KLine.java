package com.stock.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KLine {
    @CsvBindByName(column = "日期")
    @CsvDate("yyyy-MM-dd")
    private LocalDate date;

    @CsvBindByName(column = "开盘价")
    private double open;

    @CsvBindByName(column = "最高价")
    private double high;

    @CsvBindByName(column = "最低价")
    private double low;

    @CsvBindByName(column = "收盘价")
    private double close;

    @CsvBindByName(column = "成交量")
    private double volume;

    @CsvBindByName(column = "成交额")
    private double amount;
}
