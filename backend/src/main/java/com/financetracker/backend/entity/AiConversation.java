package com.financetracker.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String role;

    private String message;

    private LocalDateTime createdAt;
}
