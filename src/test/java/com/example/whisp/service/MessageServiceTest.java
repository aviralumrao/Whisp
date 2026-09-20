package com.example.whisp.service;

import com.example.whisp.dto.MessageDTO;
import com.example.whisp.exception.BadRequestException;
import com.example.whisp.exception.ResourceNotFoundException;
import com.example.whisp.exception.UnauthorizedException;
import com.example.whisp.mapper.MessageMapper;
import com.example.whisp.model.Message;
import com.example.whisp.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private Repository repository;

    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private MessageService messageService;

    private UUID messageId;
    private Message message;
    private MessageDTO messageDTO;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        message = new Message();
        message.setId(messageId);
        message.setSender("alice");
        message.setContent("Hello World");
        message.setTimestamp(LocalDateTime.now());

        messageDTO = new MessageDTO();
        messageDTO.setId(messageId);
        messageDTO.setSender("alice");
        messageDTO.setContent("Hello World");
        messageDTO.setTimestamp(message.getTimestamp());
    }

    @Test
    void testGetMessageById_Success() {
        when(repository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageMapper.toDTO(message)).thenReturn(messageDTO);

        MessageDTO result = messageService.getMessageById(messageId);

        assertNotNull(result);
        assertEquals(messageId, result.getId());
    }

    @Test
    void testGetMessageById_NotFound() {
        when(repository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.getMessageById(messageId));
    }

    @Test
    void testDeleteMessage_Success() {
        when(repository.findById(messageId)).thenReturn(Optional.of(message));

        assertDoesNotThrow(() -> messageService.deleteMessage(messageId, "alice"));
        verify(repository).delete(message);
    }

    @Test
    void testDeleteMessage_NotFound() {
        when(repository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.deleteMessage(messageId, "alice"));
    }

    @Test
    void testDeleteMessage_Unauthorized() {
        when(repository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(UnauthorizedException.class, () -> messageService.deleteMessage(messageId, "bob"));
        verify(repository, never()).delete(any());
    }

    @Test
    void testEditMessage_Success() {
        MessageDTO editRequest = new MessageDTO();
        editRequest.setId(messageId);
        editRequest.setSender("alice");
        editRequest.setContent("Updated content");

        when(repository.findById(messageId)).thenReturn(Optional.of(message));
        when(repository.save(any(Message.class))).thenReturn(message);
        when(messageMapper.toDTO(any(Message.class))).thenReturn(editRequest);

        MessageDTO result = messageService.editMessage(messageId, editRequest);

        assertNotNull(result);
        assertEquals("Updated content", result.getContent());
    }

    @Test
    void testEditMessage_NotFound() {
        MessageDTO editRequest = new MessageDTO();
        editRequest.setSender("alice");

        when(repository.findById(messageId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.editMessage(messageId, editRequest));
    }

    @Test
    void testEditMessage_Unauthorized() {
        MessageDTO editRequest = new MessageDTO();
        editRequest.setSender("bob");

        when(repository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(UnauthorizedException.class, () -> messageService.editMessage(messageId, editRequest));
    }

    @Test
    void testEditMessage_ExpiredWindow() {
        message.setTimestamp(LocalDateTime.now().minusMinutes(5));
        MessageDTO editRequest = new MessageDTO();
        editRequest.setSender("alice");
        editRequest.setContent("Updated content");

        when(repository.findById(messageId)).thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class, () -> messageService.editMessage(messageId, editRequest));
    }
}
