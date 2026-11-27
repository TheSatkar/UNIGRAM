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
 * JDBC Data Access Object for direct conversations.
 */
public class ConversationJdbcDao {

    public boolean insert(Connection conn, Conversation conv) throws SQLException {
        String sql = "INSERT INTO conversations (conversation_id, participant_one_id, participant_two_id, created_at) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conv.getConversationId());
            stmt.setString(2, conv.getParticipantOneId());
            stmt.setString(3, conv.getParticipantTwoId());
            stmt.setTimestamp(4, Timestamp.valueOf(conv.getCreatedAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<Conversation> findById(Connection conn, String conversationId) throws SQLException {
        String sql = "SELECT conversation_id, participant_one_id, participant_two_id, created_at "
                + "FROM conversations WHERE conversation_id = ?";
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

    public List<Conversation> findByParticipant(Connection conn, String studentId) throws SQLException {
        String sql = "SELECT conversation_id, participant_one_id, participant_two_id, created_at "
                + "FROM conversations WHERE participant_one_id = ? OR participant_two_id = ? "
                + "ORDER BY created_at DESC";
        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * Finds an existing direct conversation between two students regardless of initiation order.
     */
    public Optional<Conversation> findByParticipants(Connection conn, String studentOneId, String studentTwoId)
            throws SQLException {
        String sql = "SELECT conversation_id, participant_one_id, participant_two_id, created_at "
                + "FROM conversations "
                + "WHERE (participant_one_id = ? AND participant_two_id = ?) "
                + "   OR (participant_one_id = ? AND participant_two_id = ?) "
                + "LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, studentOneId);
            stmt.setString(2, studentTwoId);
            stmt.setString(3, studentTwoId);
            stmt.setString(4, studentOneId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Conversation> findAll(Connection conn) throws SQLException {
        String sql = "SELECT conversation_id, participant_one_id, participant_two_id, created_at "
                + "FROM conversations ORDER BY created_at DESC";
        List<Conversation> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Generates the next simple sequential conversation ID (conv_1, conv_2, ...).
     */
    public String generateNextConversationId(Connection conn) throws SQLException {
        String sql = "SELECT conversation_id FROM conversations";
        int max = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("conversation_id");
                if (id != null && id.startsWith("conv_")) {
                    try {
                        int num = Integer.parseInt(id.substring(5));
                        if (num > max) {
                            max = num;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return "conv_" + (max + 1);
    }

    private Conversation mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Conversation(
                rs.getString("conversation_id"),
                rs.getString("participant_one_id"),
                rs.getString("participant_two_id"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
