package com.stock.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ConceptInfo {
    private String conceptCode;
    private String conceptName;
    private List<String> stocks;
}
