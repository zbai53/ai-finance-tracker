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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${anthropic.api-key}")
    private String apiKey;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Asks Claude to pick the best matching category.
     * Returns "Other" on any error so callers never need to handle failures.
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

            String prompt = """
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
                    """.formatted(description, amount.toPlainString(), type,
                    categoryList.isEmpty() ? "(none)" : categoryList);

            MessageCreateParams params = MessageCreateParams.builder()
                    .model("claude-sonnet-4-5")
                    .maxTokens(50L)
                    .addUserMessage(prompt)
                    .build();

            Message response = withRetry(() -> buildClient().messages().create(params), "categorize");
            logTokenUsage("categorize", response);

            String result = response.content().stream()
                    .filter(ContentBlock::isText)
                    .findFirst()
                    .map(block -> block.asText().text().strip())
                    .orElse("Other");

            log.info("AI categorized '{}' ({}) → '{}'", description, type, result);
            return result;

        } catch (RuntimeException e) {
            log.warn("AI categorization failed for '{}': {}", description, friendlyError(e));
            return "Other";
        }
    }

    /**
     * Streams a Claude response chunk-by-chunk into the SSE emitter.
     * Always calls either {@code emitter.complete()} or {@code emitter.completeWithError()}.
     */
    public void streamResponse(String prompt, SseEmitter emitter) {
        doStream(prompt, emitter, null, "stream");
    }

    /**
     * Same as {@link #streamResponse} but also returns the full accumulated text,
     * so callers can persist the answer after streaming.
     */
    public String streamResponseAndCapture(String prompt, SseEmitter emitter) {
        StringBuilder captured = new StringBuilder();
        doStream(prompt, emitter, captured, "stream+capture");
        return captured.toString();
    }

    /**
     * Calls Claude non-streaming and returns the full text.
     * Intended for structured extraction (e.g. JSON intent parsing).
     */
    public String extractText(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-sonnet-4-5")
                .maxTokens(256L)
                .addUserMessage(prompt)
                .build();

        Message response = withRetry(() -> buildClient().messages().create(params), "extractText");
        logTokenUsage("extractText", response);

        return response.content().stream()
                .filter(ContentBlock::isText)
                .findFirst()
                .map(block -> block.asText().text().strip())
                .orElseThrow(() -> new IllegalStateException("No text content in Claude response"));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Core streaming implementation shared by {@link #streamResponse} and
     * {@link #streamResponseAndCapture}. When {@code captured} is non-null,
     * each text delta is appended to it in addition to being forwarded to the emitter.
     */
    private void doStream(String prompt, SseEmitter emitter,
                          StringBuilder captured, String operationName) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model("claude-sonnet-4-5")
                    .maxTokens(1000L)
                    .addUserMessage(prompt)
                    .build();

            try (var stream = buildClient().messages().createStreaming(params)) {
                stream.stream()
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .forEach(textDelta -> {
                            String text = textDelta.text();
                            if (captured != null) captured.append(text);
                            try {
                                emitter.send(text);
                            } catch (Exception sendEx) {
                                throw new RuntimeException("SSE send failed", sendEx);
                            }
                        });
            }

            emitter.complete();
            log.info("AI '{}' completed for prompt (first 80): '{}'",
                    operationName, prompt.substring(0, Math.min(prompt.length(), 80)));

        } catch (RuntimeException e) {
            log.warn("AI '{}' error — {}", operationName, friendlyError(e));
            if (isRateLimitError(e)) {
                log.warn("Rate limit hit during streaming for '{}'", operationName);
            } else if (isAuthError(e)) {
                log.error("Authentication error during streaming — check ANTHROPIC_API_KEY");
            } else if (isTimeoutError(e)) {
                log.warn("Timeout during streaming for '{}'", operationName);
            }
            emitter.completeWithError(e);
        }
    }

    /**
     * Wraps an action with one automatic retry on rate-limit (HTTP 429).
     * All other exceptions propagate immediately.
     */
    private <T> T withRetry(Supplier<T> action, String operationName) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            if (!isRateLimitError(e)) throw e;

            log.warn("Rate limit hit for '{}' (attempt 1), waiting 2 s before retry…", operationName);
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during rate-limit retry wait", ie);
            }
            log.info("Retrying '{}' after rate-limit pause (attempt 2)", operationName);
            return action.get();   // second attempt — propagates on failure
        }
    }

    private AnthropicClient buildClient() {
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    private void logTokenUsage(String operationName, Message response) {
        try {
            log.info("AI call '{}' used {} input tokens, {} output tokens",
                    operationName,
                    response.usage().inputTokens(),
                    response.usage().outputTokens());
        } catch (Exception e) {
            log.debug("Could not read token usage for '{}'", operationName);
        }
    }

    // ── Error classification ─────────────────────────────────────────────────

    private static boolean isRateLimitError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("429")
                || msg.toLowerCase().contains("rate_limit")
                || msg.toLowerCase().contains("rate limit");
    }

    private static boolean isAuthError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("401")
                || msg.toLowerCase().contains("authentication")
                || msg.toLowerCase().contains("api_key")
                || msg.toLowerCase().contains("api key");
    }

    private static boolean isTimeoutError(Throwable e) {
        String name = e.getClass().getSimpleName().toLowerCase();
        String msg  = e.getMessage();
        return name.contains("timeout") || name.contains("socket")
                || (msg != null && msg.toLowerCase().contains("timeout"));
    }

    /** Maps an exception to a user-friendly log string without exposing internals. */
    private static String friendlyError(Throwable e) {
        if (isRateLimitError(e))  return "rate limit reached";
        if (isAuthError(e))       return "authentication error — check API key";
        if (isTimeoutError(e))    return "request timed out after 30 s";
        return "unexpected error: " + e.getClass().getSimpleName();
    }
}
