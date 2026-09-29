package com.campus.servicedesk.controller;

import com.campus.servicedesk.dto.AssignTicketRequest;
import com.campus.servicedesk.dto.TicketResponse;
import com.campus.servicedesk.dto.TicketStatsResponse;
import com.campus.servicedesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only ticket management and statistics")
public class AdminController {

    private final TicketService ticketService;

    public AdminController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PatchMapping("/tickets/{id}/assign")
    @Operation(summary = "Assign a ticket to a support agent")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        TicketResponse response = ticketService.assignTicket(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get ticket statistics dashboard")
    public ResponseEntity<TicketStatsResponse> getStatistics() {
        TicketStatsResponse stats = ticketService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
