package com.campus.servicedesk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Ticket statistics returned for ADMIN dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsResponse {
    private long totalTickets;
    private long unassignedTickets;
    private Map<String, Long> ticketsByStatus;
}
