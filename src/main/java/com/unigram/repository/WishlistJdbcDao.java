package com.unigram.repository;

import com.unigram.model.WishlistItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user wishlist operations using JDBC.
 */
public class WishlistJdbcDao {

    public boolean add(Connection conn, String userId, long listingId) throws SQLException {
        String sql = "INSERT IGNORE INTO wishlist (user_id, listing_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setLong(2, listingId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean remove(Connection conn, String userId, long listingId) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setLong(2, listingId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Long> findListingIdsByUserId(Connection conn, String userId) throws SQLException {
        String sql = "SELECT listing_id FROM wishlist WHERE user_id = ? ORDER BY created_at DESC";
        List<Long> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong("listing_id"));
                }
            }
        }
        return list;
    }

    public boolean exists(Connection conn, String userId, long listingId) throws SQLException {
        String sql = "SELECT 1 FROM wishlist WHERE user_id = ? AND listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setLong(2, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
