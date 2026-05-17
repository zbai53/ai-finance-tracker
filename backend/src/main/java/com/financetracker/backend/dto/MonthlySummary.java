package com.financetracker.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlySummary {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal net;
}
