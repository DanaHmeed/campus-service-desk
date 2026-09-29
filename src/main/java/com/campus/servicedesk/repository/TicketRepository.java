package com.campus.servicedesk.repository;

import com.campus.servicedesk.entity.Ticket;
import com.campus.servicedesk.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByCreatorId(Long creatorId, Pageable pageable);

    Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByCreatorIdAndStatus(Long creatorId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByAssignedAgentIdAndStatus(Long agentId, TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedAgent IS NULL AND t.status = 'OPEN'")
    long countUnassigned();

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    java.util.List<Object[]> countByStatusGrouped();

    @Query("SELECT t FROM Ticket t WHERE " +
           "(:creatorId IS NULL OR t.creator.id = :creatorId) AND " +
           "(:agentId IS NULL OR t.assignedAgent.id = :agentId) AND " +
           "(:status IS NULL OR t.status = :status)")
    Page<Ticket> findFiltered(
            @Param("creatorId") Long creatorId,
            @Param("agentId") Long agentId,
            @Param("status") TicketStatus status,
            Pageable pageable);
}
