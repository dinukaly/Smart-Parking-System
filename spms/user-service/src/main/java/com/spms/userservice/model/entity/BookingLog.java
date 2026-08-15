package com.spms.userservice.model.entity;

import com.spms.userservice.model.entity.enums.BookingAction;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "booking_logs", indexes = {
    @Index(name = "idx_booking_logs_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reservation_id", nullable = false)
    private String reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private BookingAction action;
}
