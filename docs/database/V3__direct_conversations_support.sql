-- =====================================================================
-- Uni-Gram Database Migration V3
-- Version: 3.0.0
-- Target: MySQL 8.0+
-- Description: Enables standalone direct student-to-student conversations
--              without requiring a marketplace product_id, while enforcing
--              uniqueness for direct conversations across both participant
--              directions and preserving all existing marketplace data.
-- Safe and Idempotent: Non-destructive checks before altering schema.
-- =====================================================================

USE unigram_db;

-- 1. Make conversations.product_id nullable so direct conversations do not require a product reference
SET @is_nullable = (
    SELECT IS_NULLABLE
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'conversations'
      AND COLUMN_NAME = 'product_id'
);

SET @alter_product_id = IF(
    @is_nullable = 'NO',
    'ALTER TABLE conversations MODIFY COLUMN product_id VARCHAR(64) NULL',
    'SELECT 1'
);

PREPARE stmt_nullable FROM @alter_product_id;
EXECUTE stmt_nullable;
DEALLOCATE PREPARE stmt_nullable;

-- 2. Add canonical direct_pair_key generated column and unique index to prevent concurrent duplicate direct conversations
SET @has_direct_pair = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'conversations'
      AND COLUMN_NAME = 'direct_pair_key'
);

SET @add_direct_pair = IF(
    @has_direct_pair = 0,
    'ALTER TABLE conversations ADD COLUMN direct_pair_key VARCHAR(130) GENERATED ALWAYS AS (CASE WHEN product_id IS NULL THEN CONCAT(LEAST(LOWER(buyer_id), LOWER(seller_id)), '':'', GREATEST(LOWER(buyer_id), LOWER(seller_id))) ELSE NULL END) VIRTUAL, ADD UNIQUE KEY uq_conv_direct_pair (direct_pair_key)',
    'SELECT 1'
);

PREPARE stmt_pair FROM @add_direct_pair;
EXECUTE stmt_pair;
DEALLOCATE PREPARE stmt_pair;
