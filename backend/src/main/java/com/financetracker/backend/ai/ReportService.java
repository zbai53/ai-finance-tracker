package com.financetracker.backend.ai;

import com.financetracker.backend.dto.MonthlySummary;
import com.financetracker.backend.entity.Transaction;
import com.financetracker.backend.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionMapper transactionMapper;
    private final AiService aiService;

    public void generateMonthlyReport(Long userId, int year, int month, SseEmitter emitter) {
        try {
            // 1. Monthly summary
            MonthlySummary summary = transactionMapper.selectMonthlySummary(userId, year, month);

            // 2. Top 10 transactions for the month
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            List<Transaction> transactions = transactionMapper
                    .selectByFilter(userId, null, null, startDate, endDate)
                    .stream()
                    .limit(10)
                    .toList();

            // 3. Build prompt
            String prompt = buildPrompt(year, month, summary, transactions);

            log.info("Generating monthly report for userId={} {}/{}", userId, year, month);

            // 4. Stream response
            aiService.streamResponse(prompt, emitter);

        } catch (Exception e) {
            log.error("Failed to generate monthly report for userId={}: {}", userId, e.getMessage());
            emitter.completeWithError(e);
        }
    }

    private String buildPrompt(int year, int month, MonthlySummary summary, List<Transaction> transactions) {
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String netLabel = summary.getNet().signum() >= 0 ? "surplus" : "deficit";

        StringBuilder txLines = new StringBuilder();
        if (transactions.isEmpty()) {
            txLines.append("  (no transactions recorded)");
        } else {
            for (Transaction tx : transactions) {
                txLines.append(String.format("  - %s: $%s (%s)%n",
                        tx.getDescription().isBlank() ? "(no description)" : tx.getDescription(),
                        tx.getAmount().toPlainString(),
                        tx.getType()));
            }
        }

        return """
               You are a friendly personal financial advisor. Analyze this month's financial data and provide:
                1. A brief summary of the month (2-3 sentences)
                2. The biggest spending area and whether it seems reasonable
                3. One specific actionable suggestion for next month
                Be concise, friendly, and specific. Use the actual numbers.
                Do NOT use markdown headings (# or ##). Use **bold** for section labels instead.

                Financial data for %s %d:
                - Total Income: $%s
                - Total Expense: $%s
                - Net: $%s (%s)

                Top transactions:
                %s"""
                .formatted(
                        monthName, year,
                        summary.getTotalIncome().toPlainString(),
                        summary.getTotalExpense().toPlainString(),
                        summary.getNet().abs().toPlainString(), netLabel,
                        txLines.toString().stripTrailing()
                );
    }
}
