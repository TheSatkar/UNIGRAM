package com.unigram.repository;

import com.unigram.model.Conversation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Conversation entities using pure JDBC.
 */
public class ConversationJdbcDao {

    public boolean insert(Connection conn, Conversation conv) throws SQLException {
        String sql = "INSERT INTO conversations (conversation_id, buyer_id, seller_id, product_id, created_at) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conv.getConversationId());
            stmt.setString(2, conv.getBuyerId());
            stmt.setString(3, conv.getSellerId());
            stmt.setString(4, conv.getProductId());
            stmt.setTimestamp(5, Timestamp.valueOf(conv.getCreatedAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<Conversation> findById(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT conversation_id, buyer_id, seller_id, product_id, created_at " +
                     "FROM conversations WHERE conversation_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToConversation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Conversation> findByParticipant(Connection conn, String userId) throws SQLException {
        String sql = "SELECT conversation_id, buyer_id, seller_id, product_id, created_at " +
                     "FROM conversations WHERE buyer_id = ? OR seller_id = ? " +
                     "ORDER BY created_at DESC";
        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToConversation(rs));
                }
            }
        }
        return list;
    }

    public Optional<Conversation> findByParticipantsAndProduct(Connection conn, String buyerId, String sellerId, String productId) throws SQLException {
        String sql = "SELECT conversation_id, buyer_id, seller_id, product_id, created_at " +
                     "FROM conversations WHERE product_id = ? " +
                     "AND ((buyer_id = ? AND seller_id = ?) OR (buyer_id = ? AND seller_id = ?))";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            stmt.setString(2, buyerId);
            stmt.setString(3, sellerId);
            stmt.setString(4, sellerId);
            stmt.setString(5, buyerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToConversation(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Conversation> findAll(Connection conn) throws SQLException {
        String sql = "SELECT conversation_id, buyer_id, seller_id, product_id, created_at FROM conversations ORDER BY created_at DESC";
        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToConversation(rs));
            }
        }
        return list;
    }

    public List<Conversation> findByUser(Connection conn, String userId) throws SQLException {
        return findByParticipant(conn, userId);
    }

    public boolean existsById(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT 1 FROM conversations WHERE conversation_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean delete(Connection conn, String conversationId) throws SQLException {
        String sql = "DELETE FROM conversations WHERE conversation_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Conversation mapRowToConversation(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Conversation(
                rs.getString("conversation_id"),
                rs.getString("buyer_id"),
                rs.getString("seller_id"),
                rs.getString("product_id"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
