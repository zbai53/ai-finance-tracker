package com.financetracker.backend.controller;

import com.financetracker.backend.ai.AiService;
import com.financetracker.backend.ai.ReportService;
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

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final ReportService reportService;

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
}
