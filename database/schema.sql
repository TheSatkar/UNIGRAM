-- =====================================================================
-- Uni-Gram Standalone Campus Messaging Platform
-- Database: unigram_standalone_db
-- Target: MySQL 8.0+
-- =====================================================================

CREATE DATABASE IF NOT EXISTS unigram_standalone_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE unigram_standalone_db;

-- 1. Registered Students
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_user_email (email),
    INDEX idx_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Direct One-to-One Student Conversations
-- The virtual generated column direct_pair_key guarantees at most one
-- conversation per pair of students regardless of who initiates it.
CREATE TABLE IF NOT EXISTS conversations (
    conversation_id VARCHAR(64) NOT NULL,
    participant_one_id VARCHAR(64) NOT NULL,
    participant_two_id VARCHAR(64) NOT NULL,
    direct_pair_key VARCHAR(130) GENERATED ALWAYS AS (
        CONCAT(
            LEAST(LOWER(participant_one_id), LOWER(participant_two_id)),
            ':',
            GREATEST(LOWER(participant_one_id), LOWER(participant_two_id))
        )
    ) VIRTUAL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (conversation_id),
    UNIQUE KEY uq_conv_direct_pair (direct_pair_key),
    CONSTRAINT fk_conv_participant_one FOREIGN KEY (participant_one_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_conv_participant_two FOREIGN KEY (participant_two_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_conv_p1 (participant_one_id),
    INDEX idx_conv_p2 (participant_two_id),
    INDEX idx_conv_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Persistent Chat Messages
CREATE TABLE IF NOT EXISTS messages (
    message_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    sender_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    sent_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (message_id),
    CONSTRAINT fk_msg_conv FOREIGN KEY (conversation_id)
        REFERENCES conversations (conversation_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_msg_conv_chronological (conversation_id, sent_at ASC),
    INDEX idx_msg_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Fictional Demo Students for Viva Presentation
INSERT INTO users (user_id, display_name, email)
VALUES
    ('student_a', 'Satkar Singh', 'satkar.singh@chitkara.edu.in'),
    ('student_b', 'Priya Sharma', 'priya.sharma@chitkara.edu.in'),
    ('student_c', 'Nisha Verma', 'nisha.verma@chitkara.edu.in'),
    ('student_d', 'Madhur Tiwari', 'madhur.tiwari@chitkara.edu.in'),
    ('student_e', 'Rohan Mehta', 'rohan.mehta@chitkara.edu.in')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name);
