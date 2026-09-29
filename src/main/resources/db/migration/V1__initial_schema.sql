-- V1: Initial schema for Campus Service Desk
-- Tables: users, tickets, ticket_comments

CREATE TABLE users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(100)  NOT NULL UNIQUE,
    full_name    VARCHAR(100)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)   NOT NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tickets (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(200)  NOT NULL,
    description       TEXT          NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    priority          VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    creator_id        BIGINT        NOT NULL,
    assigned_agent_id BIGINT        NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NULL,

    CONSTRAINT fk_ticket_creator FOREIGN KEY (creator_id) REFERENCES users(id),
    CONSTRAINT fk_ticket_agent   FOREIGN KEY (assigned_agent_id) REFERENCES users(id),

    INDEX idx_tickets_creator (creator_id),
    INDEX idx_tickets_agent (assigned_agent_id),
    INDEX idx_tickets_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ticket_comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    content    TEXT   NOT NULL,
    ticket_id  BIGINT NOT NULL,
    author_id  BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comment_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users(id),

    INDEX idx_comments_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
