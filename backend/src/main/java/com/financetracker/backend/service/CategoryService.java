package com.financetracker.backend.service;

import com.financetracker.backend.entity.Category;
import com.financetracker.backend.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService{
    private final CategoryMapper categoryMapper;

    @Transactional
    public Category create(Long userId, Category category){
        category.setUserId(userId);
        categoryMapper.insert(category);
        log.info("Category created for user {}: {}", userId, category.getName());
        return category;
    }

    public List<Category> list(Long userId){
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByAsc("type", "name");
        return categoryMapper.selectList(wrapper);
    }

    @Transactional
   public void delete(Long userId, Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("Category not found");
        }
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }
        categoryMapper.deleteById(id);
        log.info("Category {} deleted by user {}", id, userId);
    }
}
