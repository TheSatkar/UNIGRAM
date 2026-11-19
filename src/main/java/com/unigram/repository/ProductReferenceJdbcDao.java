package com.unigram.repository;

import com.unigram.model.ProductReference;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for ProductReference entities using pure JDBC.
 */
public class ProductReferenceJdbcDao {

    public boolean insert(Connection conn, ProductReference product) throws SQLException {
        String sql = "INSERT INTO product_references (product_id, product_title) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getProductId());
            stmt.setString(2, product.getProductTitle());
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<ProductReference> findById(Connection conn, String productId) throws SQLException {
        String sql = "SELECT product_id, product_title FROM product_references WHERE product_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToProduct(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<ProductReference> findAll(Connection conn) throws SQLException {
        String sql = "SELECT product_id, product_title FROM product_references ORDER BY created_at ASC";
        List<ProductReference> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToProduct(rs));
            }
        }
        return list;
    }

    public boolean update(Connection conn, ProductReference product) throws SQLException {
        String sql = "UPDATE product_references SET product_title = ? WHERE product_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getProductTitle());
            stmt.setString(2, product.getProductId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean existsById(Connection conn, String productId) throws SQLException {
        String sql = "SELECT 1 FROM product_references WHERE product_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int count(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM product_references";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean delete(Connection conn, String productId) throws SQLException {
        String sql = "DELETE FROM product_references WHERE product_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    private ProductReference mapRowToProduct(ResultSet rs) throws SQLException {
        return new ProductReference(
                rs.getString("product_id"),
                rs.getString("product_title")
        );
    }
}
