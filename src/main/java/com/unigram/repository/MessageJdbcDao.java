package com.unigram.repository;

import com.unigram.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Data Access Object for chat messages, ordered chronologically.
 */
public class MessageJdbcDao {

    public boolean insert(Connection conn, Message message) throws SQLException {
        String sql = "INSERT INTO messages (message_id, conversation_id, sender_id, content, sent_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getMessageId());
            stmt.setString(2, message.getConversationId());
            stmt.setString(3, message.getSenderId());
            stmt.setString(4, message.getContent());
            stmt.setTimestamp(5, Timestamp.valueOf(message.getSentAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Message> findByConversationId(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT message_id, conversation_id, sender_id, content, sent_at "
                + "FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
        List<Message> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Optional<Message> findLatestByConversationId(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT message_id, conversation_id, sender_id, content, sent_at "
                + "FROM messages WHERE conversation_id = ? ORDER BY sent_at DESC LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int countByConversationId(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE conversation_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("sent_at");
        return new Message(
                rs.getString("message_id"),
                rs.getString("conversation_id"),
                rs.getString("sender_id"),
                rs.getString("content"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
