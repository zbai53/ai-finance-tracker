package com.financetracker.backend.controller;

import com.financetracker.backend.common.Result;
import com.financetracker.backend.dto.CategoryStatistics;
import com.financetracker.backend.dto.MonthlySummary;
import com.financetracker.backend.entity.User;
import com.financetracker.backend.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController{
    private final TransactionMapper transactionMapper;

    @GetMapping("/monthly")
    public Result<MonthlySummary> monthly(
            @RequestParam Integer year,
            @RequestParam Integer month){
        Long userId = getCurrentUserId();
        return Result.success(
                transactionMapper.selectMonthlySummary(userId, year, month)
        );
    }

    @GetMapping("/by-category")
    public Result<List<CategoryStatistics>> byCategory(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Long userId = getCurrentUserId();
        return Result.success(
                transactionMapper.selectCategoryStatistics(userId, type, startDate, endDate)
        );
    }

    private Long getCurrentUserId() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getId();
    }
}
