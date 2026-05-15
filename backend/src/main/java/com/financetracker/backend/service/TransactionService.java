package com.financetracker.backend.service;

import com.financetracker.backend.entity.Transaction;
import com.financetracker.backend.mapper.TransactionMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService{
    private final TransactionMapper transactionMapper;

    @Transactional
    public Transaction create(Long userId, Transaction transaction){
        transaction.setUserId(userId);
        transactionMapper.insert(transaction);
        log.info("Transaction created for user {}: {}", userId, transaction.getId());
        return transaction;
    }

    public PageInfo<Transaction> list(Long userId, int page, int size,
                                       String type, Long categoryId,
                                       LocalDate startDate, LocalDate endDate) {
        PageHelper.startPage(page, size);
        List<Transaction> transactions = transactionMapper.selectByFilter(
            userId, type, categoryId, startDate, endDate
        );
        return new PageInfo<>(transactions);

    }

    @Transactional
    public Transaction update(Long userId, Long id, Transaction transaction){
        Transaction existing = getOwnedTransaction(userId, id);
        transaction.setId(existing.getId());
        transaction.setUserId(userId);
        transactionMapper.updateById(transaction);
        return transactionMapper.selectById(id);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        getOwnedTransaction(userId, id);
        transactionMapper.deleteById(id);
        log.info("Transaction {} deleted by user {}", id, userId);
    }

    private Transaction getOwnedTransaction(Long userId, Long id) {
        Transaction transaction = transactionMapper.selectById(id);
        if (transaction == null) {
            throw new RuntimeException("Transaction not found");
        }
        if (!transaction.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        return transaction;
    }
}
