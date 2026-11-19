-- =====================================================================
-- UniMart + Uni-Gram Database Rollback Script U2
-- Rolls back migration V2 back to V1 base schema.
-- WARNING: This will drop tables `wishlist`, `listing_pending_edits`,
--          `listing_photos`, and `listings`. Backup data before executing!
-- =====================================================================

USE unigram_db;

DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS listing_pending_edits;
DROP TABLE IF EXISTS listing_photos;
DROP TABLE IF EXISTS listings;

-- Remove added columns from users if needed
ALTER TABLE users DROP COLUMN IF EXISTS department;
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users DROP COLUMN IF EXISTS student_id;
ALTER TABLE users DROP COLUMN IF EXISTS role;
