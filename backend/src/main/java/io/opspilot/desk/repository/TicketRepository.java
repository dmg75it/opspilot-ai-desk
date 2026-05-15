package io.opspilot.desk.repository;

import io.opspilot.desk.entity.Ticket;
import io.opspilot.desk.entity.TicketPriority;
import io.opspilot.desk.entity.TicketStatus;
import io.opspilot.desk.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByAssignedToAndStatusNot(User user, TicketStatus status, Pageable pageable);

    List<Ticket> findTop10ByOrderByUpdatedAtDesc();

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countGroupByPriority();

    boolean existsByExternalRef(String externalRef);
}
