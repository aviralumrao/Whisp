package com.example.whisp.repository;

import com.example.whisp.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface Repository extends JpaRepository<Message, UUID> {

	List<Message> findAllByOrderByTimestampAsc();
}
