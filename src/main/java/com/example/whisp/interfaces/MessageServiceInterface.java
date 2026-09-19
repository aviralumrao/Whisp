package com.example.whisp.interfaces;

import com.example.whisp.dto.MessageDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface MessageServiceInterface{

	Page<MessageDTO> getAllMessages(int page, int size);
	MessageDTO sendMessage(MessageDTO messageDTO);
	MessageDTO getMessageById(UUID id);
	void deleteMessage(UUID id, String sender);
	MessageDTO editMessage(UUID id, MessageDTO messageDTO);
}
