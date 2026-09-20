package com.example.whisp.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsErrorResponse {

    private String error;
    private String message;
    private LocalDateTime timestamp;
}
