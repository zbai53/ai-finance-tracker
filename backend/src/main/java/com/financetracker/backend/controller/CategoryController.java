package com.financetracker.backend.controller;

import com.financetracker.backend.common.Result;
import com.financetracker.backend.entity.Category;
import com.financetracker.backend.entity.User;
import com.financetracker.backend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public Result<Category> create(@Valid @RequestBody Category category) {
        return Result.success(categoryService.create(getCurrentUserId(), category));
    }

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.list(getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(getCurrentUserId(), id);
        return Result.success();
    }

    private Long getCurrentUserId() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getId();
    }
}
