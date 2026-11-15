package org.myStore.dao;

import org.myStore.db.dbConnection;
import org.myStore.entities.Product;

import java.sql.*;
import java.util.ArrayList;

public class ProductDAO {


    public ArrayList<Product> getAllProducts() {

        ArrayList<Product> products = new ArrayList<>();

        String query = "SELECT * FROM products";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query);
                ResultSet rs = pst.executeQuery()
        ) {

            while (rs.next()) {

                Product p = new Product();

                p.setProductId(rs.getInt("product_id"));
                p.setProductName(rs.getString("product_name"));
                p.setCategory(rs.getString("category"));
                p.setPrice(rs.getBigDecimal("price"));
                p.setQuantity(rs.getInt("quantity"));
                p.setReorderLevel(rs.getInt("reorder_level"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                p.setSku(rs.getString("sku"));

                products.add(p);
            }

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return products;
    }

    public int addProduct(Product product) throws SQLException {

        String query = "INSERT INTO products " +
                "(product_name, sku, category, price, quantity, reorder_level) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(
                        query, Statement.RETURN_GENERATED_KEYS)
        ) {

            pst.setString(1, product.getProductName());
            pst.setString(2, product.getSku());
            pst.setString(3, product.getCategory());
            pst.setBigDecimal(4, product.getPrice());
            pst.setInt(5, product.getQuantity());
            pst.setInt(6, product.getReorderLevel());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected == 0) {
                return -1;
            }

            // Get the auto-generated product ID
            try (ResultSet rs = pst.getGeneratedKeys()) {

                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return -1;
    }

    // Check whether SKU belongs to another product
    public boolean skuExistsForOtherProduct(String sku, int productId)
            throws SQLException {

        String query = "SELECT 1 FROM products WHERE sku = ? AND product_id != ?";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, sku);
            pst.setInt(2, productId);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }


    // Update existing product
    public boolean updateProduct(Product product) throws SQLException {

        String query = """
            UPDATE products
            SET product_name = ?,
                sku = ?,
                category = ?,
                price = ?,
                reorder_level = ?
            WHERE product_id = ?
            """;

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, product.getProductName());
            pst.setString(2, product.getSku());
            pst.setString(3, product.getCategory());
            pst.setBigDecimal(4, product.getPrice());
            pst.setInt(5, product.getReorderLevel());
            pst.setInt(6, product.getProductId());

            return pst.executeUpdate() > 0;
        }
    }

    public static boolean skuExists(String sku) throws SQLException {

        String query = "SELECT 1 FROM products WHERE sku = ?";

        try (
                Connection con = dbConnection.getConnection();
                PreparedStatement pst = con.prepareStatement(query)
        ) {

            pst.setString(1, sku);

            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

}
