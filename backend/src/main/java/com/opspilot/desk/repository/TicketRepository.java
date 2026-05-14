package com.opspilot.desk.repository;

import com.opspilot.desk.entity.AppUser;
import com.opspilot.desk.entity.Ticket;
import com.opspilot.desk.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {
    boolean existsByExternalRef(String externalRef);

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo = :user AND t.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Ticket> findOpenTicketsByAssignee(AppUser user);

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countByStatus();

    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countByPriority();

    List<Ticket> findTop10ByOrderByUpdatedAtDesc();

    long countByStatus(TicketStatus status);
}
