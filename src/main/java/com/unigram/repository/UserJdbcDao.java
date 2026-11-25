package com.unigram.repository;

import com.unigram.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for User entities using pure JDBC.
 * Employs PreparedStatement to guarantee protection against SQL injection.
 */
public class UserJdbcDao {

    /**
     * Inserts a new user record.
     */
    public boolean insert(Connection conn, User user) throws SQLException {
        String sqlFull = "INSERT INTO users (user_id, display_name, email, role, student_id, password_hash) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlFull)) {
            stmt.setString(1, user.getUserId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail() != null ? user.getEmail() : "");
            stmt.setString(4, user.getRole() != null ? user.getRole() : "student");
            stmt.setString(5, user.getStudentId() != null ? user.getStudentId() : "");
            stmt.setString(6, user.getPasswordHash() != null ? user.getPasswordHash() : "");
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Graceful fallback if role/student_id/password_hash columns do not exist (Phase 1-5 legacy schema)
            String sqlLegacy = "INSERT INTO users (user_id, display_name, email) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlLegacy)) {
                stmt.setString(1, user.getUserId());
                stmt.setString(2, user.getFullName());
                stmt.setString(3, user.getEmail() != null ? user.getEmail() : "");
                return stmt.executeUpdate() > 0;
            }
        }
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> findById(Connection conn, String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all users.
     */
    public List<User> findAll(Connection conn) throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at ASC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        }
        return list;
    }

    /**
     * Updates an existing user's display name and email.
     */
    public boolean update(Connection conn, User user) throws SQLException {
        String sql = "UPDATE users SET display_name = ?, email = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail() != null ? user.getEmail() : "");
            stmt.setString(3, user.getUserId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing user's password hash.
     */
    public boolean updatePassword(Connection conn, String userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash != null ? passwordHash : "");
            stmt.setString(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Graceful fallback if password_hash column is not present
            return false;
        }
    }

    /**
     * Checks if a user exists by ID.
     */
    public boolean existsById(Connection conn, String userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Counts total number of users in the table.
     */
    public int count(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Deletes a user by ID.
     */
    public boolean delete(Connection conn, String userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Finds a user by email.
     */
    public Optional<User> findByEmail(Connection conn, String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User(
                rs.getString("user_id"),
                rs.getString("display_name"),
                rs.getString("email")
        );
        try {
            String role = rs.getString("role");
            if (role != null) u.setRole(role);
        } catch (SQLException ignored) {}
        try {
            String sid = rs.getString("student_id");
            if (sid != null) u.setStudentId(sid);
        } catch (SQLException ignored) {}
        try {
            String ph = rs.getString("password_hash");
            if (ph != null) u.setPasswordHash(ph);
        } catch (SQLException ignored) {}
        return u;
    }
}
