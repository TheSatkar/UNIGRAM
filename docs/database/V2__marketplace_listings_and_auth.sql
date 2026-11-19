-- =====================================================================
-- UniMart + Uni-Gram Database Migration V2
-- Version: 2.0.0
-- Target: MySQL 8.0+
-- Description: Adds auth credential fields, marketplace listings,
--              listing photos, pending moderation edits, and wishlists.
-- Safe and Idempotent: Uses IF NOT EXISTS and column existence checks.
-- =====================================================================

USE unigram_db;

-- 1. Upgrade `users` table with auth and university profile fields
-- Safe column addition procedure for MySQL 8.0+
SET @dbname = DATABASE();
SET @tablename = "users";

-- Add role column
SET @columnname = "role";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE users ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'student' AFTER email"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add student_id column
SET @columnname = "student_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE users ADD COLUMN student_id VARCHAR(64) DEFAULT '' AFTER role"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add password_hash column
SET @columnname = "password_hash";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) DEFAULT '' AFTER student_id"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add department column
SET @columnname = "department";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE users ADD COLUMN department VARCHAR(128) DEFAULT NULL AFTER password_hash"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;


-- 2. Table: listings
-- Authoritative persistent storage for Chitkara student marketplace listings
CREATE TABLE IF NOT EXISTS listings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seller_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category_id VARCHAR(64) NOT NULL,
    subcategory_id VARCHAR(64) DEFAULT NULL,
    price DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2) DEFAULT NULL,
    item_condition VARCHAR(64) NOT NULL,
    meetup_spot VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    has_notes BOOLEAN NOT NULL DEFAULT FALSE,
    badge VARCHAR(64) DEFAULT 'Verified',
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    pending_type VARCHAR(32) DEFAULT 'new',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_listing_seller FOREIGN KEY (seller_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    INDEX idx_listing_seller (seller_id),
    INDEX idx_listing_status (status),
    INDEX idx_listing_category (category_id),
    INDEX idx_listing_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. Table: listing_photos
-- High-resolution image references associated with listings
CREATE TABLE IF NOT EXISTS listing_photos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    url MEDIUMTEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_photo_listing FOREIGN KEY (listing_id)
        REFERENCES listings (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    INDEX idx_photo_listing_order (listing_id, sort_order ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. Table: listing_pending_edits
-- Staged snapshot for listing edits requiring administrative moderation
CREATE TABLE IF NOT EXISTS listing_pending_edits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    proposed_changes JSON NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT fk_edit_listing FOREIGN KEY (listing_id)
        REFERENCES listings (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    UNIQUE KEY uq_pending_edit_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. Table: wishlist
-- Student bookmarks for marketplace listings
CREATE TABLE IF NOT EXISTS wishlist (
    user_id VARCHAR(64) NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, listing_id),
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_listing FOREIGN KEY (listing_id)
        REFERENCES listings (id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    INDEX idx_wishlist_user (user_id),
    INDEX idx_wishlist_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
