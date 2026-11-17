
package org.myStore.dao;

import org.myStore.db.dbConnection;
import org.myStore.entities.DashboardStats;

import java.math.BigDecimal;
import java.sql.*;

public class DashboardDAO {

    public DashboardStats getDashboardStats(
            int userId, boolean isAdmin) throws SQLException {

        long totalProducts;
        long totalUnits;
        long lowStockProducts;
        long outOfStockProducts;
        BigDecimal inventoryValue;

        String inventoryQuery = """
                SELECT
                    COUNT(*) AS total_products,
                    COALESCE(SUM(quantity), 0) AS total_units,
                    COALESCE(SUM(
                        CASE WHEN quantity <= reorder_level
                        THEN 1 ELSE 0 END
                    ), 0) AS low_stock_products,
                    COALESCE(SUM(
                        CASE WHEN quantity = 0
                        THEN 1 ELSE 0 END
                    ), 0) AS out_of_stock_products,
                    COALESCE(SUM(price * quantity), 0)
                        AS inventory_value
                FROM products
                """;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst =
                     con.prepareStatement(inventoryQuery);
             ResultSet rs = pst.executeQuery()) {

            if (!rs.next()) {
                throw new SQLException("Unable to retrieve inventory statistics.");
            }

            totalProducts = rs.getLong("total_products");
            totalUnits = rs.getLong("total_units");
            lowStockProducts = rs.getLong("low_stock_products");
            outOfStockProducts = rs.getLong("out_of_stock_products");
            inventoryValue = rs.getBigDecimal("inventory_value");
        }

        // Admin sees all pending requests.
        // Staff sees only their own pending requests.

        String requestQuery;

        if (isAdmin) {
            requestQuery = """
                    SELECT COUNT(*)
                    FROM stock_requests
                    WHERE status = 'PENDING'
                    """;
        } else {
            requestQuery = """
                    SELECT COUNT(*)
                    FROM stock_requests
                    WHERE status = 'PENDING'
                    AND requested_by = ?
                    """;
        }

        long pendingRequests;

        try (Connection con = dbConnection.getConnection();
             PreparedStatement pst =
                     con.prepareStatement(requestQuery)) {

            if (!isAdmin) {
                pst.setInt(1, userId);
            }

            try (ResultSet rs = pst.executeQuery()) {
                rs.next();
                pendingRequests = rs.getLong(1);
            }
        }

        return new DashboardStats(
                totalProducts,
                totalUnits,
                lowStockProducts,
                outOfStockProducts,
                pendingRequests,
                inventoryValue
        );
    }
}
