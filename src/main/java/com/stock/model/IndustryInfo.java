package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class IndustryInfo {
    private String industryCode;
    private String industryName;
    private String belongToSector;
    private String analysis;
}
