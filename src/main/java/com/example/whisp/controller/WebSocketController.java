package com.example.whisp.controller;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.interfaces.MessageServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

	private final MessageServiceInterface messageService;
	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat")
	public void sendMessage(MessageDTO messageDTO) {
		MessageDTO saved = messageService.sendMessage(messageDTO);
		messagingTemplate.convertAndSend("/topic/messages", saved);
	}

	@MessageMapping("/history")
	public void getHistory(MessageDTO requestDTO) {
		int page = (requestDTO != null && requestDTO.getPage() != null) ? requestDTO.getPage() : 0;
		int size = (requestDTO != null && requestDTO.getSize() != null && requestDTO.getSize() > 0) ? requestDTO.getSize() : 20;
		Page<MessageDTO> messages = messageService.getAllMessages(page, size);
		messagingTemplate.convertAndSend("/topic/history", messages);
	}

	@MessageMapping("/edit")
	public void editMessage(MessageDTO messageDTO) {
		MessageDTO updated = messageService.editMessage(messageDTO.getId(), messageDTO);
		messagingTemplate.convertAndSend("/topic/edit", updated);
	}

	@MessageMapping("/delete")
	public void deleteMessage(MessageDTO messageDTO) {
		messageService.deleteMessage(messageDTO.getId(), messageDTO.getSender());
		messagingTemplate.convertAndSend("/topic/delete", messageDTO.getId());
	}
}
