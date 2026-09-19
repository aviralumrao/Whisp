package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.service.Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class Controller {

	private final Service service;

	@GetMapping
	public ResponseEntity<Page<MessageDTO>> getAllMessages(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(service.getAllMessages(page, size));
	}

	@PostMapping
	public ResponseEntity<MessageDTO> sendMessage(@Valid @RequestBody MessageDTO messageDTO){
		MessageDTO saved = service.sendMessage(messageDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID id){
		return ResponseEntity.ok(service.getMessageById(id));
	}
}
