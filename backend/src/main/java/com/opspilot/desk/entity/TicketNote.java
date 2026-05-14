package com.opspilot.desk.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketNote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false, length = 10000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteVisibility visibility;

    @CreationTimestamp
    private Instant createdAt;
}
