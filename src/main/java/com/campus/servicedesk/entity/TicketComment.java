package com.campus.servicedesk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Comment on a support ticket.
 * <p>
 * Relationships:
 * - Many TicketComments → one Ticket
 * - Many TicketComments → one User (author)
 */
@Entity
@Table(name = "ticket_comments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TicketComment() {}

    public TicketComment(Long id, String content, Ticket ticket, User author, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.ticket = ticket;
        this.author = author;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static TicketCommentBuilder builder() { return new TicketCommentBuilder(); }

    public static class TicketCommentBuilder {
        private Long id;
        private String content;
        private Ticket ticket;
        private User author;
        private LocalDateTime createdAt;

        public TicketCommentBuilder id(Long id) { this.id = id; return this; }
        public TicketCommentBuilder content(String content) { this.content = content; return this; }
        public TicketCommentBuilder ticket(Ticket ticket) { this.ticket = ticket; return this; }
        public TicketCommentBuilder author(User author) { this.author = author; return this; }
        public TicketCommentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TicketComment build() {
            return new TicketComment(id, content, ticket, author, createdAt);
        }
    }
}
