-- =====================================================================
-- Uni-Gram: Database Schema Setup Script
-- Database: unigram_test_db
-- Target: MySQL 8.0+
-- Storage Engine: InnoDB | Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci
-- =====================================================================

-- 1. Create dedicated database if it does not already exist
CREATE DATABASE IF NOT EXISTS unigram_test_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE unigram_test_db;

-- 2. Table: users
-- Represents student participant references in Uni-Gram.
-- Authentication is decoupled: no passwords or credential tables exist here.
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) DEFAULT '',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    INDEX idx_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Table: product_references
-- Lightweight references to marketplace listings.
-- Stores only identity and snapshot title; does not replicate marketplace catalogues.
CREATE TABLE IF NOT EXISTS product_references (
    product_id VARCHAR(64) NOT NULL,
    product_title VARCHAR(255) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (product_id),
    INDEX idx_product_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Table: conversations
-- Represents a one-to-one conversation thread tied to a specific product listing.
CREATE TABLE IF NOT EXISTS conversations (
    conversation_id VARCHAR(64) NOT NULL,
    buyer_id VARCHAR(64) NOT NULL,
    seller_id VARCHAR(64) NOT NULL,
    product_id VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (conversation_id),
    CONSTRAINT fk_conv_buyer FOREIGN KEY (buyer_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_conv_seller FOREIGN KEY (seller_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_conv_product FOREIGN KEY (product_id)
        REFERENCES product_references (product_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    -- Prevent duplicate conversations between the exact same buyer, seller, and product
    CONSTRAINT uq_conv_participants_product UNIQUE (buyer_id, seller_id, product_id),
    INDEX idx_conv_buyer (buyer_id),
    INDEX idx_conv_seller (seller_id),
    INDEX idx_conv_product (product_id),
    INDEX idx_conv_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Table: messages
-- Individual messages associated with an active conversation thread.
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
    -- Composite index optimized for chronological retrieval of conversation messages
    INDEX idx_msg_conv_chronological (conversation_id, sent_at ASC),
    INDEX idx_msg_sender (sender_id),
    INDEX idx_msg_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
