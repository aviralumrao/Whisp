package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.interfaces.MessageServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MessageController {

	private final MessageServiceInterface messageService;

	@GetMapping
	public ResponseEntity<Page<MessageDTO>> getMessages(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Page<MessageDTO> messages = messageService.getAllMessages(page, size);
		return ResponseEntity.ok(messages);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID id) {
		MessageDTO message = messageService.getMessageById(id);
		return ResponseEntity.ok(message);
	}
}
