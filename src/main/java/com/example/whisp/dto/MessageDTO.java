package com.example.whisp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

	@NotBlank(message = "Username cannot be blank")
	@Size(max = 20, message = "Username must not exceed 20 characters")
	private String sender;

	@NotBlank(message = "Message content cannot be blank")
	@Size(max = 400, message = "Message content must not exceed 400 characters")
	private String content;

	private LocalDateTime timestamp;

	private Integer page;
	private Integer size;
}
