package com.campus.servicedesk.entity;

/**
 * Roles available in the Campus Service Desk system.
 * <p>
 * STUDENT – can create tickets and view their own.
 * SUPPORT_AGENT – can view assigned tickets, comment, and update status.
 * ADMIN – can assign tickets, view all tickets, and see statistics.
 */
public enum Role {
    STUDENT,
    SUPPORT_AGENT,
    ADMIN
}
