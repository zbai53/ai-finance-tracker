package com.financetracker.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financetracker.backend.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction> {

    List<Transaction> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Transaction> selectByFilter(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
