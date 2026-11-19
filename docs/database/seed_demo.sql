-- =====================================================================
-- Uni-Gram: Demonstration Seed Script
-- Database: unigram_db
-- Target: MySQL 8.0+
-- Inserts initial demo students, marketplace listings, and sample chat
-- =====================================================================

USE unigram_db;

-- 1. Insert Demo Student Users
INSERT INTO users (user_id, display_name, email, created_at)
VALUES 
    ('usr_student_A', 'Alex Chen', 'alex.chen@university.edu', '2026-11-01 10:00:00.000'),
    ('usr_student_B', 'Maya Patel', 'maya.patel@university.edu', '2026-11-01 10:05:00.000'),
    ('usr_student_C', 'Liam O''Connor', 'liam.oconnor@university.edu', '2026-11-02 11:00:00.000')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), email = VALUES(email);

-- 2. Insert Demo Marketplace Product References
INSERT INTO product_references (product_id, product_title, created_at)
VALUES 
    ('prod_cs201_book', 'Algorithms & Data Structures (4th Edition)', '2026-11-01 10:15:00.000'),
    ('prod_calc_ti84', 'TI-84 Plus Graphing Calculator', '2026-11-01 10:20:00.000')
ON DUPLICATE KEY UPDATE product_title = VALUES(product_title);

-- 3. Insert Demo Conversation between Alex (Buyer) and Maya (Seller) for Algorithms Book
INSERT INTO conversations (conversation_id, buyer_id, seller_id, product_id, created_at)
VALUES 
    ('conv_seed_001', 'usr_student_A', 'usr_student_B', 'prod_cs201_book', '2026-11-01 12:00:00.000')
ON DUPLICATE KEY UPDATE product_id = VALUES(product_id);

-- 4. Insert Demo Message Thread
INSERT INTO messages (message_id, conversation_id, sender_id, content, sent_at)
VALUES 
    ('msg_seed_001', 'conv_seed_001', 'usr_student_A', 'Hi Maya, is your Algorithms textbook still available?', '2026-11-01 12:01:00.000'),
    ('msg_seed_002', 'conv_seed_001', 'usr_student_B', 'Hi Alex! Yes, it is in great condition. Can meet at the science library.', '2026-11-01 12:03:00.000'),
    ('msg_seed_003', 'conv_seed_001', 'usr_student_A', 'Perfect! Does 4:30 PM today work for you?', '2026-11-01 12:05:00.000')
ON DUPLICATE KEY UPDATE content = VALUES(content);
