package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.service.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

	private final Service service;
	private final SimpMessagingTemplate messagingTemplate;

	@SubscribeMapping("/topic/messages")
	public List<MessageDTO> getPreviousMessages() {
		return service.getAllMessages();
	}

	@MessageMapping("/chat")
	public void sendMessage(MessageDTO message) {
		MessageDTO saved = service.sendMessage(message);
		messagingTemplate.convertAndSend("/topic/messages", saved);
	}
}
