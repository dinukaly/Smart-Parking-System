package com.spms.parkingservice.repository;

import com.spms.parkingservice.model.entity.IotEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IotEventRepository extends JpaRepository<IotEvent, UUID> {

    Page<IotEvent> findByParkingSpaceIdOrderByCreatedAtDesc(UUID parkingSpaceId, Pageable pageable);

    List<IotEvent> findByParkingSpaceIdOrderByCreatedAtDesc(UUID parkingSpaceId);
}
