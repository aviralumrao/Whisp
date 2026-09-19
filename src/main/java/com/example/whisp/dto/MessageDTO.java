package com.example.whisp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

	private UUID id;

	@NotNull
	private String sender;

	@NotNull
	private String content;

	private LocalDateTime timestamp;

	private Integer page;
	private Integer size;
}
