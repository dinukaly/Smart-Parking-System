package com.spms.userservice.repository;

import com.spms.userservice.model.entity.BookingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingLogRepository extends JpaRepository<BookingLog, UUID> {

    List<BookingLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
