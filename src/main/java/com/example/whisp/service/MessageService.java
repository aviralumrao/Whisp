package com.example.whisp.service;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.mapper.MessageMapper;
import com.example.whisp.model.Message;
import com.example.whisp.repository.Repository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessageService {

	private final Repository repository;
	private final MessageMapper messageMapper;

	public Page<MessageDTO> getAllMessages(int page, int size) {
		Pageable pageable = PageRequest.of(page, size);
		return repository.findAllByOrderByTimestampAsc(pageable)
				.map(messageMapper::toDTO);
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

	public void deleteMessage(UUID id, String sender) {
		Message message = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Message not found with id = " + id));

		if (!message.getSender().equals(sender)) {
			throw new RuntimeException("Unauthorized: You can only delete your own messages");
		}

		repository.delete(message);
	}


	public MessageDTO editMessage(UUID id, MessageDTO messageDTO) {
		Message message = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Message not found"));

		if (!message.getSender().equals(messageDTO.getSender())) {
			throw new RuntimeException("Unauthorized");
		}

		if (message.getTimestamp().isBefore(LocalDateTime.now().minusMinutes(1))) {
			throw new RuntimeException("Cannot edit");
		}

		message.setContent(messageDTO.getContent());
		Message updated = repository.save(message);
		return messageMapper.toDTO(updated);
	}

}
