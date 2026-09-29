package com.campus.servicedesk.controller;

import com.campus.servicedesk.dto.*;
import com.campus.servicedesk.entity.TicketStatus;
import com.campus.servicedesk.entity.User;
import com.campus.servicedesk.exception.BadRequestException;
import com.campus.servicedesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tickets", description = "Ticket CRUD, assignment, status, and comments")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a new ticket (STUDENT only)")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal User currentUser) {
        TicketResponse response = ticketService.createTicket(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List tickets (filtered by role)")
    public ResponseEntity<Page<TicketResponse>> listTickets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TicketStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = TicketStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status filter: " + status);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TicketResponse> tickets = ticketService.listTickets(currentUser, statusFilter, pageable);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details with comments")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        TicketResponse response = ticketService.getTicketById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'ADMIN')")
    @Operation(summary = "Update ticket status (AGENT or ADMIN)")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        TicketResponse response = ticketService.updateStatus(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal User currentUser) {
        CommentResponse response = ticketService.addComment(id, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "List comments for a ticket")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        List<CommentResponse> comments = ticketService.getComments(id, currentUser);
        return ResponseEntity.ok(comments);
    }
}
