package com.campus.servicedesk.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Support ticket entity.
 * <p>
 * Relationships:
 * - Many Tickets → one User (creator, a STUDENT)
 * - Many Tickets → one User (assignedAgent, a SUPPORT_AGENT — nullable)
 * - One Ticket → many TicketComments
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TicketPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketComment> comments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Ticket() {}

    public Ticket(Long id, String title, String description, TicketStatus status,
                  TicketPriority priority, User creator, User assignedAgent,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.creator = creator;
        this.assignedAgent = assignedAgent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = TicketStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public User getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(User assignedAgent) { this.assignedAgent = assignedAgent; }

    public List<TicketComment> getComments() { return comments; }
    public void setComments(List<TicketComment> comments) { this.comments = comments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static TicketBuilder builder() { return new TicketBuilder(); }

    public static class TicketBuilder {
        private Long id;
        private String title;
        private String description;
        private TicketStatus status;
        private TicketPriority priority;
        private User creator;
        private User assignedAgent;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public TicketBuilder id(Long id) { this.id = id; return this; }
        public TicketBuilder title(String title) { this.title = title; return this; }
        public TicketBuilder description(String description) { this.description = description; return this; }
        public TicketBuilder status(TicketStatus status) { this.status = status; return this; }
        public TicketBuilder priority(TicketPriority priority) { this.priority = priority; return this; }
        public TicketBuilder creator(User creator) { this.creator = creator; return this; }
        public TicketBuilder assignedAgent(User assignedAgent) { this.assignedAgent = assignedAgent; return this; }
        public TicketBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TicketBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Ticket build() {
            return new Ticket(id, title, description, status, priority, creator, assignedAgent, createdAt, updatedAt);
        }
    }
}
