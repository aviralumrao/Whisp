package com.example.whisp.service;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.mapper.MessageMapper;
import com.example.whisp.model.Message;
import com.example.whisp.repository.Repository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class Service {

	private final Repository repository;
	private final MessageMapper messageMapper;

	public List<MessageDTO> getAllMessages() {
		return repository.findAllByOrderByTimestampAsc()
				.stream()
				.map(messageMapper::toDTO)
				.toList();
	}

	public MessageDTO sendMessage(MessageDTO messageDTO) {
		Message message = messageMapper.toEntity(messageDTO);
		Message saved = repository.save(message);
		return messageMapper.toDTO(saved);
	}

	public MessageDTO getMessageById(UUID id) {
		Message message = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Message not found with id = " + id));
		return messageMapper.toDTO(message);
	}
}
