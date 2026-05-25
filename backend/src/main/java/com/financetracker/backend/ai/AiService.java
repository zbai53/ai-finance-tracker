package com.financetracker.backend.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.financetracker.backend.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${anthropic.api-key}")
    private String apiKey;

    /**
     * Asks Claude to pick the best matching category from the user's existing
     * categories. Returns "Other" if nothing matches or on any error.
     */
    public String categorizeTransaction(String description,
                                        BigDecimal amount,
                                        String type,
                                        List<Category> existingCategories) {
        try {
            List<Category> sameType = existingCategories.stream()
                    .filter(c -> type.equalsIgnoreCase(c.getType()))
                    .collect(Collectors.toList());

            String categoryList = sameType.stream()
                    .map(Category::getName)
                    .collect(Collectors.joining(", "));

            String prompt = String.format(
                    """
                    You are a personal finance assistant. Categorize the following transaction.

                    Transaction details:
                    - Description: %s
                    - Amount: %s
                    - Type: %s

                    Available categories: %s

                    Instructions:
                    - Pick exactly ONE category from the list above that best fits this transaction.
                    - If none of the categories fit, reply with: Other
                    - Reply with ONLY the category name. No explanation, no punctuation, no extra words.
                    """,
                    description,
                    amount.toPlainString(),
                    type,
                    categoryList.isEmpty() ? "(none)" : categoryList
            );

            AnthropicClient client = buildClient();

            Message response = client.messages().create(
                    MessageCreateParams.builder()
                            .model("claude-sonnet-4-5")
                            .maxTokens(50L)
                            .addUserMessage(prompt)
                            .build()
            );

            String result = response.content().stream()
                    .filter(ContentBlock::isText)
                    .findFirst()
                    .map(block -> block.asText().text().strip())
                    .orElse("Other");

            log.info("AI categorized transaction '{}' ({}) as '{}'", description, type, result);
            return result;

        } catch (Exception e) {
            log.warn("AI categorization failed for '{}': {}", description, e.getMessage());
            return "Other";
        }
    }

    private AnthropicClient buildClient() {
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
