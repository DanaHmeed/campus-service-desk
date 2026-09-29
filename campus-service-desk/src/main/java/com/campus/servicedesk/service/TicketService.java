package com.campus.servicedesk.service;

import com.campus.servicedesk.dto.*;
import com.campus.servicedesk.entity.*;
import com.campus.servicedesk.exception.BadRequestException;
import com.campus.servicedesk.exception.ForbiddenException;
import com.campus.servicedesk.exception.ResourceNotFoundException;
import com.campus.servicedesk.repository.TicketCommentRepository;
import com.campus.servicedesk.repository.TicketRepository;
import com.campus.servicedesk.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         TicketCommentRepository commentRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    // ── Create ───────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, User creator) {
        TicketPriority priority = TicketPriority.MEDIUM;
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            try {
                priority = TicketPriority.valueOf(request.getPriority().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid priority: " + request.getPriority());
            }
        }

        Ticket ticket = Ticket.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .priority(priority)
                .status(TicketStatus.OPEN)
                .creator(creator)
                .build();

        ticket = ticketRepository.save(ticket);
        return toResponse(ticket, false);
    }

    // ── Read ─────────────────────────────────────────────────────────────

    public TicketResponse getTicketById(Long ticketId, User currentUser) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceReadAccess(ticket, currentUser);
        return toResponse(ticket, true);
    }

    public Page<TicketResponse> listTickets(User currentUser, TicketStatus statusFilter, Pageable pageable) {
        Role role = currentUser.getRole();

        if (role == Role.STUDENT) {
            if (statusFilter != null) {
                return ticketRepository.findByCreatorIdAndStatus(currentUser.getId(), statusFilter, pageable)
                        .map(t -> toResponse(t, false));
            }
            return ticketRepository.findByCreatorId(currentUser.getId(), pageable)
                    .map(t -> toResponse(t, false));
        }

        if (role == Role.SUPPORT_AGENT) {
            if (statusFilter != null) {
                return ticketRepository.findByAssignedAgentIdAndStatus(currentUser.getId(), statusFilter, pageable)
                        .map(t -> toResponse(t, false));
            }
            return ticketRepository.findByAssignedAgentId(currentUser.getId(), pageable)
                    .map(t -> toResponse(t, false));
        }

        // ADMIN: sees all tickets, with optional status filter
        if (statusFilter != null) {
            return ticketRepository.findByStatus(statusFilter, pageable)
                    .map(t -> toResponse(t, false));
        }
        return ticketRepository.findAll(pageable)
                .map(t -> toResponse(t, false));
    }

    // ── Assign ───────────────────────────────────────────────────────────

    @Transactional
    public TicketResponse assignTicket(Long ticketId, AssignTicketRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.getAgentId()));

        if (agent.getRole() != Role.SUPPORT_AGENT) {
            throw new BadRequestException("User " + request.getAgentId() + " is not a support agent");
        }

        ticket.setAssignedAgent(agent);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket = ticketRepository.save(ticket);
        return toResponse(ticket, false);
    }

    // ── Status Update ────────────────────────────────────────────────────

    @Transactional
    public TicketResponse updateStatus(Long ticketId, UpdateStatusRequest request, User currentUser) {
        Ticket ticket = findTicketOrThrow(ticketId);

        // Only the assigned agent or an admin can change status
        boolean isAssignedAgent = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAssignedAgent && !isAdmin) {
            throw new ForbiddenException("Only the assigned agent or an admin can update ticket status");
        }

        TicketStatus newStatus;
        try {
            newStatus = TicketStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        ticket.setStatus(newStatus);
        ticket = ticketRepository.save(ticket);
        return toResponse(ticket, false);
    }

    // ── Comments ─────────────────────────────────────────────────────────

    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, User author) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceCommentAccess(ticket, author);

        TicketComment comment = TicketComment.builder()
                .content(request.getContent().trim())
                .ticket(ticket)
                .author(author)
                .build();

        comment = commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public List<CommentResponse> getComments(Long ticketId, User currentUser) {
        Ticket ticket = findTicketOrThrow(ticketId);
        enforceReadAccess(ticket, currentUser);

        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    // ── Statistics (Admin) ───────────────────────────────────────────────

    public TicketStatsResponse getStatistics() {
        long total = ticketRepository.count();
        long unassigned = ticketRepository.countUnassigned();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : ticketRepository.countByStatusGrouped()) {
            byStatus.put(((TicketStatus) row[0]).name(), (Long) row[1]);
        }

        return TicketStatsResponse.builder()
                .totalTickets(total)
                .unassignedTickets(unassigned)
                .ticketsByStatus(byStatus)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));
    }

    /**
     * Access rules for reading a ticket:
     * - STUDENT can only see their own tickets
     * - SUPPORT_AGENT can see tickets assigned to them
     * - ADMIN can see any ticket
     */
    private void enforceReadAccess(Ticket ticket, User currentUser) {
        Role role = currentUser.getRole();
        if (role == Role.ADMIN) return;

        if (role == Role.STUDENT && !ticket.getCreator().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only view your own tickets");
        }

        if (role == Role.SUPPORT_AGENT) {
            boolean isAssigned = ticket.getAssignedAgent() != null
                    && ticket.getAssignedAgent().getId().equals(currentUser.getId());
            if (!isAssigned) {
                throw new ForbiddenException("You can only view tickets assigned to you");
            }
        }
    }

    /**
     * Comment access: ticket creator, assigned agent, or admin.
     */
    private void enforceCommentAccess(Ticket ticket, User user) {
        Role role = user.getRole();
        if (role == Role.ADMIN) return;

        boolean isCreator = ticket.getCreator().getId().equals(user.getId());
        boolean isAssigned = ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getId().equals(user.getId());

        if (!isCreator && !isAssigned) {
            throw new ForbiddenException("You do not have permission to comment on this ticket");
        }
    }

    private TicketResponse toResponse(Ticket ticket, boolean includeComments) {
        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus().name())
                .priority(ticket.getPriority().name())
                .creatorId(ticket.getCreator().getId())
                .creatorName(ticket.getCreator().getFullName())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt());

        if (ticket.getAssignedAgent() != null) {
            builder.assignedAgentId(ticket.getAssignedAgent().getId())
                   .assignedAgentName(ticket.getAssignedAgent().getFullName());
        }

        if (includeComments) {
            List<CommentResponse> comments = commentRepository
                    .findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                    .map(this::toCommentResponse)
                    .collect(Collectors.toList());
            builder.comments(comments);
        }

        return builder.build();
    }

    private CommentResponse toCommentResponse(TicketComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
