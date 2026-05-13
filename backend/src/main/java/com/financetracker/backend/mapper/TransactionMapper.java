package com.financetracker.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.financetracker.backend.entity.Transaction;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransactionMapper extends BaseMapper<Transaction>{
    List<Transaction> selectByUserIdAndDateRange(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    );
}
