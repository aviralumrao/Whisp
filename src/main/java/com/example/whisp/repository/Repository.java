package com.example.whisp.repository;

import com.example.whisp.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface Repository extends JpaRepository<Message, UUID> {

	Page<Message> findAllByOrderByTimestampDesc(Pageable pageable);
}
