package com.unigram.repository;

import com.unigram.model.Listing;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for marketplace listings using pure JDBC PreparedStatement queries.
 */
public class ListingJdbcDao {

    public boolean existsById(Connection conn, long id) throws SQLException {
        String sql = "SELECT 1 FROM listings WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int count(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM listings";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public void insertWithId(Connection conn, Listing listing) throws SQLException {
        String sql = "INSERT INTO listings (id, seller_id, title, category_id, subcategory_id, price, original_price, " +
                "item_condition, meetup_spot, description, has_notes, badge, status, pending_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE id = id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listing.getId());
            stmt.setString(2, listing.getSellerId());
            stmt.setString(3, listing.getTitle());
            stmt.setString(4, listing.getCategoryId());
            stmt.setString(5, listing.getSubcategoryId());
            stmt.setBigDecimal(6, listing.getPrice());
            stmt.setBigDecimal(7, listing.getOriginalPrice());
            stmt.setString(8, listing.getItemCondition());
            stmt.setString(9, listing.getMeetupSpot());
            stmt.setString(10, listing.getDescription());
            stmt.setBoolean(11, listing.isHasNotes());
            stmt.setString(12, listing.getBadge() != null ? listing.getBadge() : "Verified");
            stmt.setString(13, listing.getStatus() != null ? listing.getStatus() : "approved");
            stmt.setString(14, listing.getPendingType() != null ? listing.getPendingType() : "new");
            stmt.executeUpdate();
        }
    }

    public long insert(Connection conn, Listing listing) throws SQLException {
        String sql = "INSERT INTO listings (seller_id, title, category_id, subcategory_id, price, original_price, " +
                "item_condition, meetup_spot, description, has_notes, badge, status, pending_type) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, listing.getSellerId());
            stmt.setString(2, listing.getTitle());
            stmt.setString(3, listing.getCategoryId());
            stmt.setString(4, listing.getSubcategoryId());
            stmt.setBigDecimal(5, listing.getPrice());
            stmt.setBigDecimal(6, listing.getOriginalPrice());
            stmt.setString(7, listing.getItemCondition());
            stmt.setString(8, listing.getMeetupSpot());
            stmt.setString(9, listing.getDescription());
            stmt.setBoolean(10, listing.isHasNotes());
            stmt.setString(11, listing.getBadge() != null ? listing.getBadge() : "Verified");
            stmt.setString(12, listing.getStatus() != null ? listing.getStatus() : "pending");
            stmt.setString(13, listing.getPendingType() != null ? listing.getPendingType() : "new");

            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    listing.setId(id);
                    return id;
                }
            }
        }
        return listing.getId();
    }

    public Optional<Listing> findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT l.*, u.full_name AS seller_name FROM listings l " +
                "LEFT JOIN users u ON l.seller_id = u.user_id WHERE l.id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Listing listing = mapRowToListing(rs);
                    listing.setPhotos(findPhotosByListingId(conn, id));
                    listing.setPendingEdit(findPendingEdit(conn, id).orElse(null));
                    return Optional.of(listing);
                }
            }
        }
        return Optional.empty();
    }

    public List<Listing> findAll(Connection conn) throws SQLException {
        String sql = "SELECT l.*, u.full_name AS seller_name, pe.proposed_changes AS pending_edit FROM listings l " +
                "LEFT JOIN users u ON l.seller_id = u.user_id " +
                "LEFT JOIN listing_pending_edits pe ON l.id = pe.listing_id " +
                "ORDER BY l.created_at DESC";
        List<Listing> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Listing l = mapRowToListing(rs);
                l.setPhotos(findPhotosByListingId(conn, l.getId()));
                try {
                    l.setPendingEdit(rs.getString("pending_edit"));
                } catch (Exception ignored) {}
                list.add(l);
            }
        }
        return list;
    }

    public boolean update(Connection conn, Listing listing) throws SQLException {
        String sql = "UPDATE listings SET title = ?, category_id = ?, subcategory_id = ?, price = ?, original_price = ?, " +
                "item_condition = ?, meetup_spot = ?, description = ?, has_notes = ?, badge = ?, status = ?, pending_type = ?, " +
                "updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, listing.getTitle());
            stmt.setString(2, listing.getCategoryId());
            stmt.setString(3, listing.getSubcategoryId());
            stmt.setBigDecimal(4, listing.getPrice());
            stmt.setBigDecimal(5, listing.getOriginalPrice());
            stmt.setString(6, listing.getItemCondition());
            stmt.setString(7, listing.getMeetupSpot());
            stmt.setString(8, listing.getDescription());
            stmt.setBoolean(9, listing.isHasNotes());
            stmt.setString(10, listing.getBadge());
            stmt.setString(11, listing.getStatus());
            stmt.setString(12, listing.getPendingType());
            stmt.setLong(13, listing.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Connection conn, long id, String status, String pendingType) throws SQLException {
        String sql = "UPDATE listings SET status = ?, pending_type = ?, updated_at = CURRENT_TIMESTAMP(3) WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, pendingType);
            stmt.setLong(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM listings WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public void insertPhoto(Connection conn, long listingId, String url, int sortOrder) throws SQLException {
        String sql = "INSERT INTO listing_photos (listing_id, url, sort_order) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            stmt.setString(2, url);
            stmt.setInt(3, sortOrder);
            stmt.executeUpdate();
        }
    }

    public List<String> findPhotosByListingId(Connection conn, long listingId) throws SQLException {
        String sql = "SELECT url FROM listing_photos WHERE listing_id = ? ORDER BY sort_order ASC";
        List<String> photos = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    photos.add(rs.getString("url"));
                }
            }
        }
        return photos;
    }

    public void deletePhotosByListingId(Connection conn, long listingId) throws SQLException {
        String sql = "DELETE FROM listing_photos WHERE listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            stmt.executeUpdate();
        }
    }

    public void savePendingEdit(Connection conn, long listingId, String proposedChangesJson) throws SQLException {
        String sql = "INSERT INTO listing_pending_edits (listing_id, proposed_changes) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE proposed_changes = VALUES(proposed_changes), created_at = CURRENT_TIMESTAMP(3)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            stmt.setString(2, proposedChangesJson);
            stmt.executeUpdate();
        }
    }

    public Optional<String> findPendingEdit(Connection conn, long listingId) throws SQLException {
        String sql = "SELECT proposed_changes FROM listing_pending_edits WHERE listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("proposed_changes"));
                }
            }
        }
        return Optional.empty();
    }

    public void deletePendingEdit(Connection conn, long listingId) throws SQLException {
        String sql = "DELETE FROM listing_pending_edits WHERE listing_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, listingId);
            stmt.executeUpdate();
        }
    }

    private Listing mapRowToListing(ResultSet rs) throws SQLException {
        Listing l = new Listing();
        l.setId(rs.getLong("id"));
        l.setSellerId(rs.getString("seller_id"));
        l.setTitle(rs.getString("title"));
        l.setCategoryId(rs.getString("category_id"));
        l.setSubcategoryId(rs.getString("subcategory_id"));
        l.setPrice(rs.getBigDecimal("price"));
        l.setOriginalPrice(rs.getBigDecimal("original_price"));
        l.setItemCondition(rs.getString("item_condition"));
        l.setMeetupSpot(rs.getString("meetup_spot"));
        l.setDescription(rs.getString("description"));
        l.setHasNotes(rs.getBoolean("has_notes"));
        l.setBadge(rs.getString("badge"));
        l.setStatus(rs.getString("status"));
        l.setPendingType(rs.getString("pending_type"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) l.setCreatedAt(createdTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) l.setUpdatedAt(updatedTs.toLocalDateTime());

        try {
            l.setSellerName(rs.getString("seller_name"));
        } catch (Exception ignored) {}

        return l;
    }
}
