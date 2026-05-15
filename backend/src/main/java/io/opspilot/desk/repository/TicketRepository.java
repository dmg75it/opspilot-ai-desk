package io.opspilot.desk.repository;

import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
    Page<Ticket> findAll(Pageable pageable);
    List<Ticket> findByAssignedToAndStatusNot(User user, Ticket.Status status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(Ticket.Status status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.priority = :priority")
    long countByPriority(Ticket.Priority priority);

    List<Ticket> findTop10ByOrderByUpdatedAtDesc();
}
