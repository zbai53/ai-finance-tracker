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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.RawMessageStreamEvent;

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

    /**
     * Streams a Claude response chunk-by-chunk into the given SSE emitter.
     * Each text delta is sent as a plain string event. The emitter is
     * completed (or completed-with-error) before this method returns.
     */
    public void streamResponse(String prompt, SseEmitter emitter) {
        try {
            AnthropicClient client = buildClient();

            MessageCreateParams params = MessageCreateParams.builder()
                    .model("claude-sonnet-4-5")
                    .maxTokens(1000L)
                    .addUserMessage(prompt)
                    .build();

            try (var streamResponse = client.messages().createStreaming(params)) {
                streamResponse.stream()
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .forEach(textDelta -> {
                            try {
                                emitter.send(textDelta.text());
                            } catch (Exception sendEx) {
                                throw new RuntimeException(sendEx);
                            }
                        });
            }

            emitter.complete();
            log.info("AI stream completed for prompt: '{}'",
                    prompt.substring(0, Math.min(prompt.length(), 80)));

        } catch (Exception e) {
            log.warn("AI stream error: {}", e.getMessage());
            emitter.completeWithError(e);
        }
    }

    /**
     * Calls Claude non-streaming and returns the full text response.
     * Intended for structured extraction tasks (e.g., JSON intent parsing).
     */
    public String extractText(String prompt) {
        AnthropicClient client = buildClient();
        Message response = client.messages().create(
                MessageCreateParams.builder()
                        .model("claude-sonnet-4-5")
                        .maxTokens(256L)
                        .addUserMessage(prompt)
                        .build()
        );
        return response.content().stream()
                .filter(ContentBlock::isText)
                .findFirst()
                .map(block -> block.asText().text().strip())
                .orElseThrow(() -> new IllegalStateException("No text content in Claude response"));
    }

    private AnthropicClient buildClient() {
        log.info("Using API key starting with: {}", apiKey != null ? apiKey.substring(0, 20) : "NULL");
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
