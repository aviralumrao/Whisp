package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

	private final MessageService messageService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat")
	public void sendMessage(MessageDTO messageDTO) {
		MessageDTO saved = messageService.sendMessage(messageDTO);
		messagingTemplate.convertAndSend("/topic/messages", saved);
	}
}
