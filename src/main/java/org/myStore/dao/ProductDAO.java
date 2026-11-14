package org.myStore.dao;

import org.myStore.db.dbConnection;
import org.myStore.entities.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

}
