package com.intellibank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String reply;
    private String modelUsed;
    private LocalDateTime timestamp;
}
