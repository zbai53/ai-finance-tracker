package com.financetracker.backend.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.financetracker.backend.entity.Category;
import com.financetracker.backend.entity.Transaction;
import com.financetracker.backend.entity.User;
import com.financetracker.backend.mapper.CategoryMapper;
import com.financetracker.backend.mapper.TransactionMapper;
import com.financetracker.backend.mapper.UserMapper;
import com.financetracker.backend.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder {

    private static final String DEMO_EMAIL    = "demo@example.com";
    private static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD = "demo123456";
    private static final long   SEED          = 42L;

    private final UserService       userService;
    private final UserMapper        userMapper;
    private final CategoryMapper    categoryMapper;
    private final TransactionMapper transactionMapper;

    @PostConstruct
    public void seed() {
        QueryWrapper<User> check = new QueryWrapper<>();
        check.eq("email", DEMO_EMAIL);
        if (userMapper.selectOne(check) != null) {
            return; // already seeded
        }

        log.info("Seeding demo account…");

        // ── 1. Create user ───────────────────────────────────────────────────
        User demo = userService.register(DEMO_USERNAME, DEMO_EMAIL, DEMO_PASSWORD);
        Long userId = demo.getId();

        // ── 2. Create categories ─────────────────────────────────────────────
        String[][] categoryDefs = {
            {"Food",          "EXPENSE"},
            {"Transport",     "EXPENSE"},
            {"Entertainment", "EXPENSE"},
            {"Shopping",      "EXPENSE"},
            {"Coffee",        "EXPENSE"},
            {"Utilities",     "EXPENSE"},
            {"Salary",        "INCOME"},
            {"Freelance",     "INCOME"},
        };

        List<Category> categories = new ArrayList<>();
        for (String[] def : categoryDefs) {
            Category cat = Category.builder()
                    .userId(userId)
                    .name(def[0])
                    .type(def[1])
                    .createdAt(LocalDateTime.now())
                    .build();
            categoryMapper.insert(cat);
            categories.add(cat);
        }

        Map<String, Long> catId = new java.util.HashMap<>();
        for (Category c : categories) catId.put(c.getName(), c.getId());

        // ── 3. Seed 90 days of transactions ──────────────────────────────────
        Random rng = new Random(SEED);
        LocalDate today = LocalDate.now();
        List<Transaction> txns = new ArrayList<>();

        // Monthly salary on the 1st of each month covered by the 90-day window
        LocalDate cursor = today.minusDays(89);
        while (!cursor.isAfter(today)) {
            if (cursor.getDayOfMonth() == 1) {
                txns.add(buildTx(userId, catId.get("Salary"), "INCOME",
                        new BigDecimal("3500.00"), "Monthly Salary", cursor, "Salary"));
            }
            cursor = cursor.plusDays(1);
        }

        // 2-3 expense transactions per week
        String[] expenseNames  = {"Food", "Coffee", "Transport", "Entertainment", "Shopping"};
        double[][] expenseRanges = {
            {15, 80},   // Food
            {5,  15},   // Coffee
            {20, 60},   // Transport
            {30, 100},  // Entertainment
            {50, 200},  // Shopping
        };
        String[] descriptions = {
            "Grocery shopping", "Lunch", "Dinner out", "Supermarket",
            "Morning coffee", "Café latte", "Coffee shop",
            "Bus fare", "Uber ride", "Gas station", "Metro card",
            "Movie tickets", "Concert", "Streaming service", "Museum",
            "Online shopping", "Clothes", "Books", "Electronics",
        };

        for (int weekOffset = 0; weekOffset < 13; weekOffset++) {
            LocalDate weekStart = today.minusDays(89L - weekOffset * 7L);
            int txnsThisWeek = 2 + rng.nextInt(2); // 2 or 3
            for (int t = 0; t < txnsThisWeek; t++) {
                LocalDate date = weekStart.plusDays(rng.nextInt(7));
                if (date.isAfter(today)) date = today;

                int catIdx  = rng.nextInt(expenseNames.length);
                String name = expenseNames[catIdx];
                double min  = expenseRanges[catIdx][0];
                double max  = expenseRanges[catIdx][1];
                double amt  = min + rng.nextDouble() * (max - min);
                BigDecimal amount = BigDecimal.valueOf(amt).setScale(2, RoundingMode.HALF_UP);

                String desc = descriptions[rng.nextInt(descriptions.length)];
                txns.add(buildTx(userId, catId.get(name), "EXPENSE", amount, desc, date, name));
            }
        }

        // Occasional freelance income (~once a month)
        for (int m = 0; m < 3; m++) {
            LocalDate date = today.minusDays(10L + m * 30L);
            double amt = 200 + rng.nextDouble() * 600; // $200-800
            BigDecimal amount = BigDecimal.valueOf(amt).setScale(2, RoundingMode.HALF_UP);
            txns.add(buildTx(userId, catId.get("Freelance"), "INCOME", amount, "Freelance project", date, "Freelance"));
        }

        for (Transaction tx : txns) transactionMapper.insert(tx);

        log.info("Demo account seeded: {} categories, {} transactions", categories.size(), txns.size());
    }

    private Transaction buildTx(Long userId, Long categoryId, String type,
                                 BigDecimal amount, String description,
                                 LocalDate date, String aiCategory) {
        return Transaction.builder()
                .userId(userId)
                .categoryId(categoryId)
                .amount(amount)
                .type(type)
                .description(description)
                .transactionDate(date)
                .aiCategory(aiCategory)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
