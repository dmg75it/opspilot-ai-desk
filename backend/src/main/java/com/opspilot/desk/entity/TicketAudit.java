package com.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ticketId;

    @Column(nullable = false)
    private String action;

    private String oldValue;
    private String newValue;
    private String fieldName;

    @Column(nullable = false)
    private UUID actorId;

    @CreationTimestamp
    private Instant createdAt;
}
