package com.financetracker.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategoryStatistics {
    private Long categoryId;
    private String categoryName;
    private BigDecimal total;
    private Long count;
}
