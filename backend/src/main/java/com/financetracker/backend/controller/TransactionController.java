package com.financetracker.backend.controller;

import com.financetracker.backend.common.Result;
import com.financetracker.backend.entity.Transaction;
import com.financetracker.backend.entity.User;
import com.financetracker.backend.service.TransactionService;
import com.github.pagehelper.PageInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController{
    private final TransactionService transactionService;

    @PostMapping
    public Result<Transaction> create(@Valid @RequestBody Transaction transaction){
        Long userId = getCurrentUserId();
        return Result.success(transactionService.create(userId, transaction));
    }

    @GetMapping
     public Result<PageInfo<Transaction>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        Long userId = getCurrentUserId();
        return Result.success(transactionService.list(
                userId, page, size, type, categoryId, startDate, endDate
        ));
    }

    @PutMapping("/{id}")
     public Result<Transaction> update(@PathVariable Long id,
                                       @Valid @RequestBody Transaction transaction) {
        Long userId = getCurrentUserId();
        return Result.success(transactionService.update(userId, id, transaction));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        transactionService.delete(userId, id);
        return Result.success();
    }

    private Long getCurrentUserId(){
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getId();
    }
}
