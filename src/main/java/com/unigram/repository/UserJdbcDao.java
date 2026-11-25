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
 * JDBC Data Access Object for student records in the users table.
 */
public class UserJdbcDao {

    public boolean insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (user_id, display_name, email) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUserId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<User> findById(Connection conn, String userId) throws SQLException {
        String sql = "SELECT user_id, display_name, email FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = "SELECT user_id, display_name, email FROM users ORDER BY created_at ASC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean existsById(Connection conn, String userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("user_id"),
                rs.getString("display_name"),
                rs.getString("email")
        );
    }
}
