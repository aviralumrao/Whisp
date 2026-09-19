package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	private final MessageService messageService;

	@GetMapping
	public ResponseEntity<Page<MessageDTO>> getAllMessages(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(messageService.getAllMessages(page, size));
	}

	@PostMapping
	public ResponseEntity<MessageDTO> sendMessage(@Valid @RequestBody MessageDTO messageDTO){
		MessageDTO saved = messageService.sendMessage(messageDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MessageDTO> getMessageById(@PathVariable UUID id){
		return ResponseEntity.ok(messageService.getMessageById(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMessage(
			@PathVariable UUID id,
			@RequestParam String sender) {
		messageService.deleteMessage(id, sender);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}")
	public ResponseEntity<MessageDTO> editMessage(
			@PathVariable UUID id,
			@Valid @RequestBody MessageDTO messageDTO) {
		return ResponseEntity.ok(messageService.editMessage(id, messageDTO));
	}
}
