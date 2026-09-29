-- V2: Seed data for local development
-- Passwords are BCrypt hashes of "password123"

INSERT INTO users (email, full_name, password_hash, role) VALUES
('student@campus.edu', 'Alice Johnson', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT'),
('student2@campus.edu', 'Bob Williams', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT'),
('agent@campus.edu', 'Carol Davis', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SUPPORT_AGENT'),
('agent2@campus.edu', 'Dave Miller', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SUPPORT_AGENT'),
('admin@campus.edu', 'Eve Wilson', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');

-- Sample tickets
INSERT INTO tickets (title, description, status, priority, creator_id, assigned_agent_id, created_at, updated_at) VALUES
('Cannot access campus Wi-Fi', 'My laptop cannot connect to CampusNet since yesterday. I have tried restarting the router and my device.', 'OPEN', 'HIGH', 1, NULL, NOW(), NOW()),
('Projector in Room 301 not working', 'The projector in lecture room 301 shows no image when connected via HDMI.', 'IN_PROGRESS', 'MEDIUM', 1, 3, NOW(), NOW()),
('Need VPN access for research', 'I need VPN credentials to access the journal database from off-campus.', 'OPEN', 'LOW', 2, NULL, NOW(), NOW()),
('Email account locked', 'My university email account is locked after too many failed login attempts.', 'RESOLVED', 'URGENT', 2, 3, NOW(), NOW()),
('Software license request — MATLAB', 'I need a MATLAB license for my engineering coursework this semester.', 'OPEN', 'MEDIUM', 1, NULL, NOW(), NOW());

-- Sample comments
INSERT INTO ticket_comments (content, ticket_id, author_id, created_at) VALUES
('I have escalated this to the network team.', 2, 3, NOW()),
('Thank you! Any ETA?', 2, 1, NOW()),
('Should be fixed by end of day.', 2, 3, NOW()),
('Your account has been unlocked. Please reset your password.', 4, 3, NOW());
