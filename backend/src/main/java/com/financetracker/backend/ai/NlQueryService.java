package com.financetracker.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.backend.entity.AiConversation;
import com.financetracker.backend.entity.Category;
import com.financetracker.backend.entity.Transaction;
import com.financetracker.backend.mapper.CategoryMapper;
import com.financetracker.backend.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlQueryService {

    private final CategoryMapper categoryMapper;
    private final TransactionMapper transactionMapper;
    private final AiService aiService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public void processQuery(Long userId, String question, SseEmitter emitter) {
        try {
            // 1. Load last 5 conversation turns for context
            List<AiConversation> history = conversationService.getRecent(userId, 5);
            String contextBlock = conversationService.buildContextString(history);

            // 2. Load user's categories
            List<Category> categories = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId)
            );

            String categoryList = categories.stream()
                    .map(c -> c.getName() + " (" + c.getType() + ")")
                    .collect(Collectors.joining(", "));

            // 3. Ask Claude to extract structured intent (non-streaming, no history needed)
            String intentPrompt = """
                    Extract the query intent as JSON only. No explanation, no markdown, no extra text.
                    Today's date is %s.
                    Categories available: %s

                    Question: %s

                    Return exactly this JSON structure (use null for unknown fields):
                    {"type": "expense or income or null", "categoryName": "exact category name or null", "startDate": "YYYY-MM-DD or null", "endDate": "YYYY-MM-DD or null"}
                    """.formatted(LocalDate.now(), categoryList.isEmpty() ? "(none)" : categoryList, question);

            String intentText = aiService.extractText(intentPrompt);
            log.info("Raw intent JSON from Claude: {}", intentText);

            // 3. Parse JSON — strip any accidental markdown fences
            Map<String, String> intent = parseIntentJson(intentText);

            String type         = blankToNull(intent.get("type"));
            String categoryName = blankToNull(intent.get("categoryName"));
            String startDateStr = blankToNull(intent.get("startDate"));
            String endDateStr   = blankToNull(intent.get("endDate"));

            // 4. Resolve category name → id
            Long categoryId = null;
            if (categoryName != null) {
                categoryId = categories.stream()
                        .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                        .findFirst()
                        .map(Category::getId)
                        .orElse(null);
            }

            LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
            LocalDate endDate   = endDateStr   != null ? LocalDate.parse(endDateStr)   : null;

            // 5. Run the pre-written MyBatis query
            List<Transaction> transactions = transactionMapper
                    .selectByFilter(userId, type, categoryId, startDate, endDate);

            log.info("NL query matched {} transactions for userId={}", transactions.size(), userId);

            // 6. Build data context, prepending conversation history
            String dataContext = contextBlock + buildDataContext(question, transactions);

            // 7. Save the user's question
            conversationService.save(userId, "user", question);

            // 8. Stream the natural-language answer and capture it for persistence
            String answer = aiService.streamResponseAndCapture(dataContext, emitter);

            // 9. Save the assistant's answer (only if non-empty — error paths return "")
            if (!answer.isBlank()) {
                conversationService.save(userId, "assistant", answer);
            }

        } catch (Exception e) {
            log.error("NL query failed for userId={}: {}", userId, e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, String> parseIntentJson(String raw) throws Exception {
        // Extract the first {...} block in case Claude adds extra text
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalArgumentException("No JSON object found in intent response: " + raw);
        }
        String json = raw.substring(start, end + 1);
        return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    }

    private String buildDataContext(String question, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return """
                    The user asked: "%s"

                    No transactions were found matching that query. Please tell the user clearly that no matching transactions exist and suggest they try a different time range or category.
                    """.formatted(question);
        }

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder lines = new StringBuilder();
        transactions.stream().limit(20).forEach(tx ->
                lines.append(String.format("  - %s: $%s (%s) on %s%n",
                        tx.getDescription().isBlank() ? "(no description)" : tx.getDescription(),
                        tx.getAmount().toPlainString(),
                        tx.getType(),
                        tx.getTransactionDate()))
        );
        if (transactions.size() > 20) {
            lines.append(String.format("  ... and %d more transactions%n", transactions.size() - 20));
        }

        return """
                The user asked: "%s"

                Here is the relevant financial data (%d transactions, total $%s):
                %s
                Please answer the user's question based on this data. Be friendly, specific, and use the actual numbers. Keep your answer concise.
                """.formatted(question, transactions.size(), total.toPlainString(), lines.toString().stripTrailing());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.strip())) return null;
        return value.strip();
    }
}
