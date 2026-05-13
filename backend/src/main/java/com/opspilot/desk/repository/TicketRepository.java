package com.opspilot.desk.repository;

import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.enums.TicketCategory;
import com.opspilot.desk.entity.enums.TicketPriority;
import com.opspilot.desk.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo WHERE t.id = :id")
    Optional<Ticket> findByIdWithUsers(@Param("id") Long id);

    @Query(value = """
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.assignedTo
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:category IS NULL OR t.category = :category)
          AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
        """,
        countQuery = """
        SELECT COUNT(t) FROM Ticket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:category IS NULL OR t.category = :category)
          AND (:assignedToId IS NULL OR t.assignedTo.id = :assignedToId)
        """)
    Page<Ticket> findWithFilters(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            @Param("assignedToId") Long assignedToId,
            Pageable pageable
    );

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo WHERE t.assignedTo.id = :userId AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Ticket> findOpenTicketsAssignedTo(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo ORDER BY t.updatedAt DESC LIMIT 10")
    List<Ticket> findTop10ByOrderByUpdatedAtDesc();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE CAST(m.createdAt AS date) = CURRENT_DATE")
    long countAiInteractionsToday();
}
