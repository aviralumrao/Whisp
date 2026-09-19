package com.example.whisp.mapper;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.model.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MessageMapper {

	public MessageDTO toDTO(Message message) {
		MessageDTO dto = new MessageDTO();
		dto.setId(message.getId());
		dto.setSender(message.getSender());
		dto.setContent(message.getContent());
		dto.setTimestamp(message.getTimestamp());
		return dto;
	}

	public Message toEntity(MessageDTO dto) {
		Message message = new Message();
		message.setSender(dto.getSender());
		message.setContent(dto.getContent());
		message.setTimestamp(LocalDateTime.now());
		return message;
	}
}
