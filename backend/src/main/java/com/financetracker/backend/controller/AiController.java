package com.financetracker.backend.controller;

import com.financetracker.backend.ai.AiService;
import com.financetracker.backend.ai.AnomalyService;
import com.financetracker.backend.ai.ConversationService;
import com.financetracker.backend.ai.NlQueryService;
import com.financetracker.backend.ai.ReportService;
import com.financetracker.backend.common.Result;
import com.financetracker.backend.entity.AiConversation;
import com.financetracker.backend.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ReportService reportService;
    private final NlQueryService nlQueryService;
    private final AnomalyService anomalyService;
    private final ConversationService conversationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> aiService.streamResponse(prompt, emitter))
                .exceptionally(ex -> {
                    log.error("Unexpected error in AI stream task", ex);
                    emitter.completeWithError(ex);
                    return null;
                });

        return emitter;
    }

    @GetMapping(value = "/report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report(@RequestParam int year, @RequestParam int month) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        SseEmitter emitter = new SseEmitter(120_000L);

        CompletableFuture.runAsync(() -> reportService.generateMonthlyReport(userId, year, month, emitter))
                .exceptionally(ex -> {
                    log.error("Unexpected error in AI report task for userId={}", userId, ex);
                    emitter.completeWithError(ex);
                    return null;
                });

        return emitter;
    }

    @GetMapping(value = "/query", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter query(@RequestParam String question) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> nlQueryService.processQuery(userId, question, emitter))
                .exceptionally(ex -> {
                    log.error("Unexpected error in NL query task for userId={}", userId, ex);
                    emitter.completeWithError(ex);
                    return null;
                });

        return emitter;
    }

    @GetMapping(value = "/anomalies", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter anomalies(@RequestParam int year, @RequestParam int month) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        SseEmitter emitter = new SseEmitter(60_000L);

        CompletableFuture.runAsync(() -> anomalyService.detectAnomalies(userId, year, month, emitter))
                .exceptionally(ex -> {
                    log.error("Unexpected error in anomaly detection for userId={}", userId, ex);
                    emitter.completeWithError(ex);
                    return null;
                });

        return emitter;
    }

    @GetMapping("/history")
    public Result<List<AiConversation>> history() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = currentUser.getId();
        List<AiConversation> history = conversationService.getRecent(userId, 20);
        return Result.success(history);
    }
}
