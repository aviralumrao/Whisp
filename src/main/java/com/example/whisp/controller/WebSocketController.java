package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.interfaces.MessageServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

	private final MessageServiceInterface messageService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat")
	public void sendMessage(@Valid MessageDTO messageDTO) {
		MessageDTO saved = messageService.sendMessage(messageDTO);
		messagingTemplate.convertAndSend("/topic/messages", saved);
	}

	@MessageMapping("/edit")
	public void editMessage(@Valid MessageDTO messageDTO) {
		MessageDTO updated = messageService.editMessage(messageDTO.getId(), messageDTO);
		messagingTemplate.convertAndSend("/topic/edit", updated);
	}

	@MessageMapping("/delete")
	public void deleteMessage(MessageDTO messageDTO) {
		messageService.deleteMessage(messageDTO.getId(), messageDTO.getSender());
		messagingTemplate.convertAndSend("/topic/delete", messageDTO.getId());
	}
}
