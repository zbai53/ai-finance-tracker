package com.financetracker.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financetracker.backend.entity.AiConversation;
import com.financetracker.backend.mapper.AiConversationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final AiConversationMapper aiConversationMapper;

    /** Persist a single turn (role = "user" or "assistant"). */
    public void save(Long userId, String role, String message) {
        AiConversation record = AiConversation.builder()
                .userId(userId)
                .role(role)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        aiConversationMapper.insert(record);
        log.debug("Saved conversation turn: userId={} role={}", userId, role);
    }

    /** Return the most recent {@code limit} turns, oldest-first for prompt inclusion. */
    public List<AiConversation> getRecent(Long userId, int limit) {
        List<AiConversation> desc = aiConversationMapper.selectList(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .orderByDesc(AiConversation::getCreatedAt)
                        .last("LIMIT " + limit)
        );
        // Reverse so the list is chronological (oldest → newest)
        Collections.reverse(desc);
        return desc;
    }

    /**
     * Format recent history into a context block suitable for prepending to a prompt.
     * Returns an empty string when there is no history yet.
     */
    public String buildContextString(List<AiConversation> history) {
        if (history.isEmpty()) return "";

        StringBuilder sb = new StringBuilder("Previous conversation context:\n");
        for (AiConversation turn : history) {
            String label = "user".equalsIgnoreCase(turn.getRole()) ? "[User]" : "[Assistant]";
            sb.append(label).append(": ").append(turn.getMessage()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}
