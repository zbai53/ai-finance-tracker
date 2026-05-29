package com.financetracker.backend.ai;

import com.financetracker.backend.dto.CategoryStatistics;
import com.financetracker.backend.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final TransactionMapper transactionMapper;
    private final AiService aiService;

    public void detectAnomalies(Long userId, int year, int month, SseEmitter emitter) {
        try {
            // 1. Current month stats
            List<CategoryStatistics> current = fetchExpenseStats(userId, year, month);

            // 2. Previous 2 months stats
            int[] prevYear1  = prevMonth(year, month);
            int[] prevYear2  = prevMonth(prevYear1[0], prevYear1[1]);
            List<CategoryStatistics> prev1 = fetchExpenseStats(userId, prevYear1[0], prevYear1[1]);
            List<CategoryStatistics> prev2 = fetchExpenseStats(userId, prevYear2[0], prevYear2[1]);

            // 3. Build per-category averages from previous 2 months
            Map<String, BigDecimal> prev1Map = toNameMap(prev1);
            Map<String, BigDecimal> prev2Map = toNameMap(prev2);

            // 4. Build prompt
            String prompt = buildPrompt(year, month, prevYear1, prevYear2, current, prev1Map, prev2Map);

            log.info("Detecting anomalies for userId={} {}/{}", userId, year, month);

            // 5. Stream Claude's analysis
            aiService.streamResponse(prompt, emitter);

        } catch (Exception e) {
            log.error("Anomaly detection failed for userId={}: {}", userId, e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<CategoryStatistics> fetchExpenseStats(Long userId, int year, int month) {
        return transactionMapper.selectCategoryStatistics(
                userId, "expense", monthStart(year, month), monthEnd(year, month));
    }

    private static LocalDate monthStart(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    private static LocalDate monthEnd(int year, int month) {
        LocalDate start = monthStart(year, month);
        return start.withDayOfMonth(start.lengthOfMonth());
    }

    /** Returns {year, month} for the month before the given one. */
    private static int[] prevMonth(int year, int month) {
        return month == 1 ? new int[]{year - 1, 12} : new int[]{year, month - 1};
    }

    private static Map<String, BigDecimal> toNameMap(List<CategoryStatistics> stats) {
        return stats.stream()
                .filter(s -> s.getCategoryName() != null)
                .collect(Collectors.toMap(
                        CategoryStatistics::getCategoryName,
                        CategoryStatistics::getTotal,
                        (a, b) -> a   // keep first on duplicate names (shouldn't happen)
                ));
    }

    private static String monthLabel(int year, int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year;
    }

    private String buildPrompt(
            int year, int month,
            int[] prevYear1, int[] prevYear2,
            List<CategoryStatistics> current,
            Map<String, BigDecimal> prev1Map,
            Map<String, BigDecimal> prev2Map) {

        String currentLabel = monthLabel(year, month);
        String prev1Label   = monthLabel(prevYear1[0], prevYear1[1]);
        String prev2Label   = monthLabel(prevYear2[0], prevYear2[1]);

        StringBuilder rows = new StringBuilder();
        rows.append(String.format("  %-22s %12s %12s %12s %12s%n",
                "Category", currentLabel, prev1Label, prev2Label, "2-Mo Avg"));
        rows.append("  ").append("-".repeat(72)).append("\n");

        for (CategoryStatistics stat : current) {
            String name = stat.getCategoryName() != null ? stat.getCategoryName() : "Uncategorized";
            BigDecimal curr = stat.getTotal();
            BigDecimal p1   = prev1Map.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal p2   = prev2Map.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal avg  = p1.add(p2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            rows.append(String.format("  %-22s %12s %12s %12s %12s%n",
                    name,
                    "$" + curr.toPlainString(),
                    "$" + p1.toPlainString(),
                    "$" + p2.toPlainString(),
                    "$" + avg.toPlainString()));
        }

        return """
                You are a personal finance advisor reviewing monthly spending patterns.

                Spending by category (expenses only):
                %s
                Instructions:
                - Identify categories where %s spending is more than 50%% above the 2-month average.
                - If a category had $0 in both previous months and has spending now, flag it as "new spending".
                - For each anomaly, state the category, the current amount, the average, and the percentage increase.
                - If there are no significant anomalies, say so clearly and positively.
                - Be friendly, specific, and concise. Do NOT use markdown headings — use **bold** for labels instead.
                """.formatted(rows.toString().stripTrailing(), currentLabel);
    }
}
